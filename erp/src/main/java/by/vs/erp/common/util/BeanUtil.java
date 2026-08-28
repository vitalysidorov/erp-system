package by.vs.erp.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class BeanUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext еще не инициализирован. " +
                    "Убедитесь, что компонент BeanUtil сканируется Spring-контекстом.");
        }
        return context.getBean(beanClass);
    }

    public static <T> T getBean(String name, Class<T> beanClass) {
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext еще не инициализирован.");
        }
        return context.getBean(name, beanClass);
    }
}

