package by.vs.erp.common.database;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
public class DataSourceAspect {
    @Before("@annotation(transactional) || @within(transactional)")
    public void proceed(Transactional transactional) {
        if (transactional != null && transactional.readOnly()) {
            DbContextHolder.set(DataSourceType.REPLICA);
        } else {
            DbContextHolder.set(DataSourceType.MASTER);
        }
    }
    @After("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void clear() { DbContextHolder.clear(); }
}

