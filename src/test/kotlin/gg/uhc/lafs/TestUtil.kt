package gg.uhc.lafs

import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing
import org.mockito.verification.VerificationMode

inline fun <reified T : Any> mock(): T = Mockito.mock(T::class.java)

fun <T> T.verify() : T = Mockito.verify(this)
fun <T> T.verify(mode: VerificationMode) : T = Mockito.verify(this, mode)

val <T> T.whenCalled: OngoingStubbing<T>
    get() = Mockito.`when`(this)
