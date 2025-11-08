package Models;

import java.util.Objects;

public class CategoryBudget {
    private final String category;
    private double limit;

    public CategoryBudget(String category, double limit) {
        this.category = category;
        this.limit = limit;
    }

    public String getCategory() { return category; }
    public double getLimit() { return limit; }
    public void setLimit(double limit) { this.limit = limit; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryBudget cb)) return false;
        return Objects.equals(category, cb.category);
    }

    @Override
    public int hashCode() { return Objects.hash(category); }
}
