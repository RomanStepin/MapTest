package com.example.maptest.ai.map

import androidx.lifecycle.ViewModel
import com.example.maptest.ai.model.Flag
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

class MapViewModel : ViewModel() {

    var activeFlagInfoNumberSubject: BehaviorSubject<Int> = BehaviorSubject.create()

    var activeFlagInfoNumber = 0
    set(value) {
        field = if (value > 3) 1 else value
        activeFlagInfoNumberSubject.onNext(field)
    }

    val flags = listOf<Flag>(
        Flag(1, "Александр", true, "05-06-2022", "04:05:22", 55.748223, 37.625486, "https://globalmsk.ru/usr/person/big-person-15629077401.jpg"),
        Flag(2, "Сергей", false, "04-06-2022", "03:01:13", 55.340805, 58.031438, "https://globalmsk.ru/usr/person/big-person-15632701381.jpg"),
        Flag(3, "Николай", false, "05-06-2022", "11:17:55", 44.826252, 24.233745, "https://globalmsk.ru/usr/person/big-person-15628393081.jpg"))

    fun getFlags (): Observable<List<Flag>> {
        return Observable.fromArray(flags)
    }

    private fun getFlagWithId(i: Int): Observable<Flag> {
        return Observable.just(flags[i-1])
    }

    fun getActiveFlagInfoNumberUpdate(): Observable<Flag> {
        return activeFlagInfoNumberSubject.flatMap {
            if (it != 0)
                getFlagWithId(it)
            else
                Observable.empty()
        }
    }
}