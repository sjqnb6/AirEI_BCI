package Extras_;

public class FilterConstants {
    public double[] a;
    public double[] b;
    public String name;
    public String short_name;
    FilterConstants(double[] b_given, double[] a_given, String name_given, String short_name_given) {
        b = new double[b_given.length];a = new double[b_given.length];
        for (int i=0; i<b.length;i++) { b[i] = b_given[i];}
        for (int i=0; i<a.length;i++) { a[i] = a_given[i];}
        name = name_given;
        short_name = short_name_given;
    }
};