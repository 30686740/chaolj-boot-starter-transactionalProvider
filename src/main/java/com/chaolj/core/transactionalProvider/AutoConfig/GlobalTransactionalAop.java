package com.chaolj.core.transactionalProvider.AutoConfig;

import cn.hutool.core.util.StrUtil;
import io.seata.core.context.RootContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.chaolj.core.MyConst;
import com.chaolj.core.MyDatalog;
import com.chaolj.core.MyUser;

@Aspect
@Component
public class GlobalTransactionalAop {
    @Pointcut("@annotation(io.seata.spring.annotation.GlobalTransactional)")
    public void Pointcut(){
    }

    @Around("Pointcut()")
    public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
        Object rtValue = null;

        var request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        var tranFirst = StrUtil.isBlank(request.getHeader(MyConst.HEADERKEY_GTTrackID));
        var tranId = RootContext.getXID();

        MyUser.setCurrentGTTrackFirst(tranFirst);
        MyUser.setCurrentGTTrackId(tranId);

        try {
            Object[] args = pjp.getArgs();
            rtValue = pjp.proceed(args);

            // 全局事务的首发者，负责最后提交缓存的数据日志
            if (tranFirst && !StrUtil.isBlank(tranId)) {
                MyDatalog.PushCache(tranId);
            }
        }
        finally {
            // 全局事务的首发者，负责最后清理缓存的数据日志
            if (tranFirst && !StrUtil.isBlank(tranId)) {
                MyDatalog.ClearCache(tranId);
            }
        }

        return rtValue;
    }
}
