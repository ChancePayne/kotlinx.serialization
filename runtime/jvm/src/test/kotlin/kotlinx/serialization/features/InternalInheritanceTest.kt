/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class InternalInheritanceTest {
    @Serializable
    open class A(val parent: Int) {
        private val rootOptional = "rootOptional"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is A) return false

            if (parent != other.parent) return false
            if (rootOptional != other.rootOptional) return false

            return true
        }

    }

    @Serializable
    open class B(val parent2: Int, @Transient val transientDerived: String = "X", val derived: String) : A(parent2) {
        protected val bodyDerived = "body"
    }

    @Serializable
    class C(val parent3: Int) : B(parent3, derived = "derived") {
        val lastDerived = "optional"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as C

            if (!super.equals(other)) return false
            if (parent3 != other.parent3) return false
            if (lastDerived != other.lastDerived) return false
            if (parent2 != other.parent2) return false
            if (transientDerived != other.transientDerived) return false
            if (derived != other.derived) return false
            if (bodyDerived != other.bodyDerived) return false

            return true
        }


    }


    @Test
    fun testStringify() {
        assertEquals(
                "{parent:42,rootOptional:rootOptional,parent2:42,derived:derived,bodyDerived:body,parent3:42,lastDerived:optional}",
                Json.unquoted.stringify(C(42))
        )
        assertEquals(
                "{parent:13,rootOptional:rootOptional,parent2:13,derived:bbb,bodyDerived:body}",
                Json.unquoted.stringify(B(13, derived = "bbb"))
        )
    }

    @Test
    fun testParse() {
        assertEquals(
                C(42),
                Json.unquoted.parse<C>("{parent:42,rootOptional:rootOptional,parent2:42,derived:derived,bodyDerived:body,parent3:42,lastDerived:optional}")
        )
        assertEquals(
                C(43),
                Json.unquoted.parse<C>("{parent:43,rootOptional:rootOptional,parent2:43,derived:derived,bodyDerived:body,parent3:43,lastDerived:optional}")
        )

    }

    @Test
    fun testParseOptionals() {
        assertEquals(
                B(100, derived = "wowstring"),
                Json.unquoted.parse<B>("{parent:100,rootOptional:rootOptional,parent2:100,derived:wowstring,bodyDerived:body}")
        )
        assertEquals(
                C(44),
                Json.unquoted.parse<C>("{parent:44, parent2:44,derived:derived,bodyDerived:body,parent3:44}")
        )
        assertEquals(
                B(101, derived = "wowstring"),
                Json.unquoted.parse<B>("{parent:101,parent2:101,derived:wowstring,bodyDerived:body}")
        )
        assertEquals(
                A(77),
                Json.unquoted.parse<A>("{parent:77,rootOptional:rootOptional}")
        )
        assertEquals(
                A(78),
                Json.unquoted.parse<A>("{parent:78}")
        )
    }

    @Test(expected = SerializationException::class)
    fun testThrowTransient() {
        Json.unquoted.parse<B>("{parent:100,rootOptional:rootOptional,transientDerived:X,parent2:100,derived:wowstring,bodyDerived:body}")
    }

    @Test(expected = SerializationException::class)
    fun testThrowMissingField() {
        Json.unquoted.parse<C>("{parent:42,rootOptional:rootOptional,derived:derived,bodyDerived:body,parent3:42,lastDerived:optional}")
    }

    @Test
    fun testSerializeAsParent() {
        val obj1: A = B(77, derived = "derived")
        val obj2: A = C(77)
        assertEquals("""{parent:77,rootOptional:rootOptional}""", Json.unquoted.stringify(obj1))
        assertEquals("""{parent:77,rootOptional:rootOptional}""", Json.unquoted.stringify(obj2))
    }
}
