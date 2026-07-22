package com.smartparking.aspect;

import com.smartparking.service.SystemLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AOP切面：自动记录关键业务操作日志
 * 拦截Controller层的主要操作，记录到数据库
 */
@Aspect
@Component
public class LogAspect {

    @Autowired
    private SystemLogService systemLogService;

    // ===== 切点定义 =====

    /** 车辆入场 */
    @Pointcut("execution(* com.smartparking.controller.VehicleController.vehicleEntry(..))")
    public void vehicleEntryPoint() {}

    /** 车辆出场 */
    @Pointcut("execution(* com.smartparking.controller.VehicleController.vehicleExit(..))")
    public void vehicleExitPoint() {}

    /** 费用计算 */
    @Pointcut("execution(* com.smartparking.controller.FeeController.calculateFee(..))")
    public void feeCalculatePoint() {}

    /** 费用支付 */
    @Pointcut("execution(* com.smartparking.controller.FeeController.payFee(..))")
    public void feePayPoint() {}

    /** 配置更新 */
    @Pointcut("execution(* com.smartparking.controller.SystemController.updateConfig(..))")
    public void configUpdatePoint() {}

    /** 住户添加 */
    @Pointcut("execution(* com.smartparking.controller.ResidentController.addResident(..))")
    public void residentAddPoint() {}

    /** 一键备份 */
    @Pointcut("execution(* com.smartparking.controller.SystemController.backup(..))")
    public void backupPoint() {}

    /** 测试数据生成 */
    @Pointcut("execution(* com.smartparking.controller.TestDataController.generateTestData(..))")
    public void testDataPoint() {}

    // ===== 后置通知（成功时记录） =====

    @AfterReturning(pointcut = "vehicleEntryPoint()", returning = "result")
    public void logVehicleEntry(JoinPoint jp, Object result) {
        Object[] args = jp.getArgs();
        String plateNumber = args.length > 0 && args[0] != null ? args[0].toString() : "未知";
        String spotNumber = args.length > 1 && args[1] != null ? args[1].toString() : "未知";
        systemLogService.log("ENTRY", "车辆" + plateNumber + "入场 - 车位" + spotNumber, plateNumber, "系统", "SUCCESS");
    }

    @AfterReturning(pointcut = "vehicleExitPoint()", returning = "result")
    public void logVehicleExit(JoinPoint jp, Object result) {
        Object[] args = jp.getArgs();
        String plateNumber = args.length > 0 && args[0] != null ? args[0].toString() : "未知";
        systemLogService.log("EXIT", "车辆" + plateNumber + "出场", plateNumber, "系统", "SUCCESS");
    }

    @AfterReturning(pointcut = "feeCalculatePoint()", returning = "result")
    public void logFeeCalculate(JoinPoint jp, Object result) {
        Object[] args = jp.getArgs();
        String plateNumber = args.length > 0 && args[0] != null ? args[0].toString() : "未知";
        systemLogService.log("FEE_CALC", "计算车辆" + plateNumber + "停车费用", plateNumber, "系统", "SUCCESS");
    }

    @AfterReturning(pointcut = "feePayPoint()", returning = "result")
    public void logFeePay(JoinPoint jp, Object result) {
        Object[] args = jp.getArgs();
        String feeId = args.length > 0 && args[0] != null ? args[0].toString() : "未知";
        systemLogService.log("FEE_PAY", "费用结算完成 - 费用ID:" + feeId, null, "系统", "SUCCESS");
    }

    @AfterReturning(pointcut = "configUpdatePoint()", returning = "result")
    public void logConfigUpdate(JoinPoint jp, Object result) {
        Object[] args = jp.getArgs();
        String configKey = args.length > 0 && args[0] != null ? args[0].toString() : "未知";
        String configValue = args.length > 1 && args[1] != null ? args[1].toString() : "未知";
        systemLogService.log("CONFIG_UPDATE", "系统配置变更: " + configKey + " = " + configValue, null, "管理员", "SUCCESS");
    }

    @AfterReturning(pointcut = "residentAddPoint()", returning = "result")
    public void logResidentAdd(JoinPoint jp, Object result) {
        Object[] args = jp.getArgs();
        String plateNumber = "未知";
        String userName = "未知";
        // ResidentController.addResident 参数是 ResidentDTO
        if (args.length > 0 && args[0] != null) {
            try {
                Object dto = args[0];
                java.lang.reflect.Method getPlate = dto.getClass().getMethod("getPlateNumber");
                java.lang.reflect.Method getName = dto.getClass().getMethod("getUserName");
                plateNumber = (String) getPlate.invoke(dto);
                userName = (String) getName.invoke(dto);
            } catch (Exception e) {
                // 反射失败，使用默认值
            }
        }
        systemLogService.log("RESIDENT_ADD", "添加住户: " + userName + " - " + plateNumber, plateNumber, "系统", "SUCCESS");
    }

    @AfterReturning(pointcut = "backupPoint()")
    public void logBackup() {
        systemLogService.log("BACKUP", "一键备份数据 - 生成Excel备份文件", null, "系统", "SUCCESS");
    }

    @AfterReturning(pointcut = "testDataPoint()")
    public void logTestData() {
        systemLogService.log("SYSTEM", "生成测试数据", null, "系统", "SUCCESS");
    }

    // ===== 异常通知（失败时记录） =====

    @AfterThrowing(pointcut = "vehicleEntryPoint()", throwing = "ex")
    public void logVehicleEntryError(JoinPoint jp, Throwable ex) {
        Object[] args = jp.getArgs();
        String plateNumber = args.length > 0 && args[0] != null ? args[0].toString() : "未知";
        systemLogService.log("ENTRY", "车辆入场失败(" + plateNumber + "): " + ex.getMessage(), plateNumber, "系统", "FAILURE");
    }

    @AfterThrowing(pointcut = "vehicleExitPoint()", throwing = "ex")
    public void logVehicleExitError(JoinPoint jp, Throwable ex) {
        Object[] args = jp.getArgs();
        String plateNumber = args.length > 0 && args[0] != null ? args[0].toString() : "未知";
        systemLogService.log("EXIT", "车辆出场失败(" + plateNumber + "): " + ex.getMessage(), plateNumber, "系统", "FAILURE");
    }

    @AfterThrowing(pointcut = "feePayPoint()", throwing = "ex")
    public void logFeePayError(JoinPoint jp, Throwable ex) {
        systemLogService.log("FEE_PAY", "费用支付失败: " + ex.getMessage(), null, "系统", "FAILURE");
    }

    @AfterThrowing(pointcut = "configUpdatePoint()", throwing = "ex")
    public void logConfigUpdateError(JoinPoint jp, Throwable ex) {
        systemLogService.log("CONFIG_UPDATE", "配置更新失败: " + ex.getMessage(), null, "管理员", "FAILURE");
    }
}