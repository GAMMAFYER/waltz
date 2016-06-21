package com.khartec.waltz.model;

import com.khartec.waltz.common.Checks;

public enum Quarter {

    Q1,
    Q2,
    Q3,
    Q4;

    public static Quarter fromInt(Integer quarter) {
        Checks.checkNotNull(quarter, "quarter cannot be null");
        switch (quarter)
        {
            case 1:
                return Q1;
            case 2:
                return Q2;
            case 3:
                return Q3;
            case 4:
                return Q4;
            default:
                throw new IllegalArgumentException("Unexpected quarter: " + quarter);
        }
    }
}