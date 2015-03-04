package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.util.Date;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean
@SessionScoped
public class TransientTest implements Serializable
{
    private static final long serialVersionUID = 1L;

    Date date = new Date();
    transient Integer a = 1;
    transient Integer b;
    transient Integer c;

    public TransientTest()
    {
	b = 2;
	c = 3;
    }

    private Object readResolve()
    {
	c = 4;
	return this;
    }

    public Date getDate()
    {
	return date;
    }

    public Integer getA()
    {
	return a;
    }

    public Integer getB()
    {
	return b;
    }

    public Integer getC()
    {
	return c;
    }

}
