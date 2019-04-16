package com.thoughtmechanix.organization.hystrix;

import com.thoughtmechanix.organization.utils.UserContext;
import com.thoughtmechanix.organization.utils.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;

/**
 * 自定义Callable，用于调用Hystrix保护的代码，并接受来之父线程的UserContext
 * @param <V>
 */
public final class DelegatingUserContextCallable<V> implements Callable<V> {
    private static final Logger logger = LoggerFactory.getLogger(DelegatingUserContextCallable.class);
    private final Callable<V> delegate;

    private UserContext originalUserContext;

    public DelegatingUserContextCallable(Callable<V> delegate,
                                             UserContext userContext) {
        Assert.notNull(delegate, "delegate cannot be null");
        Assert.notNull(userContext, "userContext cannot be null");
        this.delegate = delegate;
        this.originalUserContext = userContext;
    }

    public DelegatingUserContextCallable(Callable<V> delegate) {
        this(delegate, UserContextHolder.getContext());
    }

    /**
     * delegate属性可以视为由@HystrixCommand注解保护的方法的句柄，
     * 执行delegate.call()即会调用@HystrixCommand保护的方法
     */
    public V call() throws Exception {
        //设置UserContext，存储UserContext的ThreadLocal变量与运行受Hystrix保护的方法的线程相关联
        UserContextHolder.setContext( originalUserContext );

        try {
            return delegate.call();
        }
        finally {
            this.originalUserContext = null;
        }
    }

    public String toString() {
        return delegate.toString();
    }


    public static <V> Callable<V> create(Callable<V> delegate,
                                         UserContext userContext) {
        return new DelegatingUserContextCallable<V>(delegate, userContext);
    }
}