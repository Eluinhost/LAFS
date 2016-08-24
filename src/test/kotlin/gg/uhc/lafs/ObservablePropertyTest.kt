package gg.uhc.lafs

import org.jetbrains.spek.api.Spek
import rx.lang.kotlin.ReplaySubject

private class TestClass {
    val observable = ReplaySubject<String>()
    var property: String by observable.delegateProperty("initial")
}

class ObservablePropertyTest : Spek({
    describe("delegateProperty") {
        it("should trigger onNext for initial value + updates") {
            val test = TestClass()

            val calledWith = mutableListOf<String>()
            test.observable.subscribe { calledWith.add(it) }

            assert(calledWith.size == 1)
            assert(calledWith[0] == "initial")

            test.property = "new"

            assert(calledWith.size == 2)
            assert(calledWith[0] == "initial")
            assert(calledWith[1] == "new")
        }
    }
})