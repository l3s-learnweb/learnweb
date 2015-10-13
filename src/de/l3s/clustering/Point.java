package de.l3s.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Point
{
    private double x = 0;
    private double y = 0;
    private int cluster_number = 0;

    public Point()
    {
    }

    public Point(double x, double y)
    {
	this.setX(x);
	this.setY(y);
    }

    public void setX(double x)
    {
	this.x = x;
    }

    public double getX()
    {
	return this.x;
    }

    public void setY(double y)
    {
	this.y = y;
    }

    public double getY()
    {
	return this.y;
    }

    public void setCluster(int n)
    {
	this.cluster_number = n;
    }

    public int getCluster()
    {
	return this.cluster_number;
    }

    //Calculates the distance between two points.
    public double distance(Point p, Point centroid)
    {
	return Math.sqrt(Math.pow((centroid.getY() - p.getY()), 2) + Math.pow((centroid.getX() - p.getX()), 2));
    }

    //Creates random point
    public Point createRandomPoint(int min, int max)
    {
	Random r = new Random();
	double x = min + (max - min) * r.nextDouble();
	double y = min + (max - min) * r.nextDouble();
	return new Point(x, y);
    }

    public List<Point> createRandomPoints(int min, int max, int number)
    {
	List<Point> points = new ArrayList<Point>(number);
	for(int i = 0; i < number; i++)
	{
	    points.add(createRandomPoint(min, max));
	}
	return points;
    }

    @Override
    public String toString()
    {
	return "(" + x + "," + y + ")";
    }
}
