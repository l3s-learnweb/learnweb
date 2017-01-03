package de.l3s.learnweb;

public class Metric
{

    private String nameP;
    private String nameA;
    private String nameE;

    private Double valueD;
    private Long valueL;
    private String valueS;

    public Metric()
    {
    }

    public Metric(String nameP, String nameA, String nameE, Double valueD)
    {
        super();
        this.nameP = nameP;
        this.nameA = nameA;
        this.nameE = nameE;
        this.valueD = valueD;
    }

    public Metric(String nameP, String nameA, String nameE, Long valueL)
    {
        super();
        this.nameP = nameP;
        this.nameA = nameA;
        this.nameE = nameE;
        this.valueL = valueL;
    }

    public Metric(String nameP, String nameA, String nameE, String valueS)
    {
        super();
        this.nameP = nameP;
        this.nameA = nameA;
        this.nameE = nameE;
        this.valueS = valueS;
    }

    public String getNameP()
    {
        return nameP;
    }

    public void setNameP(String nameP)
    {
        this.nameP = nameP;
    }

    public String getNameA()
    {
        return nameA;
    }

    public void setNameA(String nameA)
    {
        this.nameA = nameA;
    }

    public String getNameE()
    {
        return nameE;
    }

    public void setNameE(String nameE)
    {
        this.nameE = nameE;
    }

    public Double getValueD()
    {
        return valueD;
    }

    public void setValueD(Double valueD)
    {
        this.valueD = valueD;
    }

    public Long getValueL()
    {
        return valueL;
    }

    public void setValueL(Long valueL)
    {
        this.valueL = valueL;
    }

    public String getValueS()
    {
        return valueS;
    }

    public void setValueS(String valueS)
    {
        this.valueS = valueS;
    }

}
