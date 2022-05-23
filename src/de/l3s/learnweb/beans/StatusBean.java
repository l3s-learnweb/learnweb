package de.l3s.learnweb.beans;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Named;
import javax.enterprise.context.RequestScoped;

import de.l3s.learnweb.Learnweb;

@Named
@RequestScoped
public class StatusBean extends ApplicationBean
{

    private List<Service> services = new LinkedList<>();

    public StatusBean()
    {
        services.add(new Service("Learnweb Tomcat", "ok", "", "Obviously OK, otherwise this page would not be reachable"));

        Learnweb learnweb = getLearnweb();

        // test learnweb database
        String status = "ok";
        String comment = "";
        try
        {
            Statement stmt = learnweb.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM lw_user");

            if(!rs.next() || rs.getInt(1) < 400)
            {
                status = "error";
                comment = "unexpected result from database";
            }
        }
        catch(SQLException e)
        {
            status = "error";
            comment = e.getMessage();
        }
        services.add(new Service("Learnweb Database", status, learnweb.getProperties().getProperty("mysql_url"), comment));

        // very simple database integrity test
        status = "ok";
        comment = "";

        try
        {
            if(!learnweb.getResourceManager().isResourceRatedByUser(2811, 1684))
            {
                status = "warning";
                comment = "unexpected result from database";
            }
        }
        catch(Exception e)
        {
            status = "error";
            comment = e.getMessage();
        }
        services.add(new Service("Learnweb Database integrity", status, learnweb.getProperties().getProperty("FEDORA_SERVER_URL"), comment));

    }

    public List<Service> getServices()
    {
        return services;
    }

    public class Service
    {
        private String name;
        private String status;
        private String url;
        private String comment;

        public Service(String name, String status, String url, String comment)
        {
            super();
            this.name = name;
            this.status = status;
            this.url = url;
            this.comment = comment;
        }

        public String getName()
        {
            return name;
        }

        public String getStatus()
        {
            return status;
        }

        public String getComment()
        {
            return comment;
        }

        public String getUrl()
        {
            return url;
        }

    }
}