package de.l3s.searchHistoryTest;

public class EntityPair {
    final String entity1, entity2;

    public EntityPair(String entity1, String entity2)
    {
            this.entity1 = entity1;
            this.entity2 = entity2;
    }

    public String getEntity1() {
            return this.entity1;
    }

    public String getEntity2() {
            return this.entity2;
    }
    
    @Override
    public int hashCode() {
            return entity1.hashCode() ^ entity2.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
            return (obj instanceof EntityPair) 
                            && ((EntityPair) obj).getEntity1().equals(entity1) 
                            && ((EntityPair) obj).getEntity2().equals(entity2);
    }

    @Override
    public String toString() {
            return "[" + entity1 + "," + entity2 + "]";
    }
}
