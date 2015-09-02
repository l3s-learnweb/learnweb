package de.l3s.archivedemo;

public class DAOFactory
{
    private static QueryDAOImpl queryDao = null;

    public static QueryDAOImpl getQueryDAO()
    {
	if(queryDao == null)
	    queryDao = new QueryDAOImpl();
	return queryDao;
    }

    public static ResultDAOImpl getResultDAO()
    {
	return new ResultDAOImpl();
    }

}
