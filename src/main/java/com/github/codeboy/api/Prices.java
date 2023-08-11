package com.github.codeboy.api;

public class Prices {
    public Prices(String students, String employees, String pupils, String others) {
        this.students = students;
        this.employees = employees;
        this.pupils = pupils;
        this.others = others;
    }

    private String students, employees, pupils, others;

    public String getStudents() {
        return students;
    }

    public String getEmployees() {
        return employees;
    }

    public String getPupils() {
        return pupils;
    }

    public String getOthers() {
        return others;
    }
}
