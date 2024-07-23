package ai.neptune.test.model;


import java.util.Arrays;

public class AddBatch {
    public String symbol;
    public Float[] values;

    public AddBatch(String symbol, Float[] values) {
        this.symbol = symbol;
        this.values = values;
    }


    public AddBatch() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Float[] getValues() {
        return values;
    }

    public void setValues(Float[] values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "AddBatch{" +
                "symbol='" + symbol + '\'' +
                ", values=" + Arrays.toString(values) +
                '}';
    }
}
