package cs.utils;

import com.google.common.base.Objects;

public class Tuple2<X, Y> {
    public final X _1;
    public final Y _2;
    public Tuple2(X _1, Y _2) {
        this._1 = _1;
        this._2 = _2;
    }
    public X _1() {
        return _1;
    }
    public Y _2() {
        return _2;
    }
    @Override
    public String toString() {
        return "Tuple2{" +
                "_1=" + _1 +
                ", _2=" + _2 +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
        return Objects.equal(_1, tuple2._1) &&
                Objects.equal(_2, tuple2._2);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(_1, _2);
    }
}