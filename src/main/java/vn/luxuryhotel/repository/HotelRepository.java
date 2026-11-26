package vn.luxuryhotel.repository;

import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import vn.luxuryhotel.exception.NotFoundException;
import java.util.*;

/** Repository for small catalogs and read projections. Values are always bound parameters. */
@Repository
public class HotelRepository {
    @PersistenceContext private EntityManager em;
    public <T> T get(Class<T> type, Long id) {
        T value=em.find(type,id);
        if(value==null) throw new NotFoundException();
        return value;
    }
    public <T> T lock(Class<T> type,Long id) {
        T value=em.find(type,id,LockModeType.PESSIMISTIC_WRITE);
        if(value==null) throw new NotFoundException();
        em.refresh(value,LockModeType.PESSIMISTIC_WRITE);
        return value;
    }
    public <T> T add(T value) { em.persist(value); return value; }
    public <T> List<T> all(Class<T> type) { return list("from "+type.getSimpleName()+" order by id",type,Map.of()); }
    public <T> List<T> list(String hql,Class<T> type,Map<String,?> parameters) {
        var query=em.createQuery(hql,type); parameters.forEach(query::setParameter); return query.getResultList();
    }
    public <T> List<T> page(String hql,Class<T> type,Map<String,?> parameters,int page,int size) {
        var query=em.createQuery(hql,type); parameters.forEach(query::setParameter);
        return query.setFirstResult(Math.max(0,page)*size).setMaxResults(size).getResultList();
    }
    public <T> T scalar(String hql,Class<T> type,Map<String,?> parameters) {
        var query=em.createQuery(hql,type); parameters.forEach(query::setParameter); return query.getSingleResult();
    }
    public void flush() { em.flush(); }
}
