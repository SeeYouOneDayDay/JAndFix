package com.tmall.wireless.jandfix;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by jingchaoqinjc on 17/5/16.
 */

public class MethodSize6_0 implements IMethodSize {

    private static int methodSize = Constants.INVALID_SIZE;
    private static int methodIndexOffset = Constants.INVALID_SIZE;

    static Field artMethodField;

    static {
        try {
            Class absMethodClass = Class.forName("java.lang.reflect.AbstractMethod");
            artMethodField = absMethodClass.getDeclaredField("artMethod");
            artMethodField.setAccessible(true);

            //init size
            Method method1 = MethodSizeCase.class.getDeclaredMethod("method1");
            Method method2 = MethodSizeCase.class.getDeclaredMethod("method2");
            Method method3 = MethodSizeCase.class.getDeclaredMethod("method3");

            long method1Addr = (long) artMethodField.get(method1);
            long method2Addr = (long) artMethodField.get(method2);
            long method3Addr = (long) artMethodField.get(method3);
            // 计算sizeof: 同一个类中ArtMethod在内存地址是按顺序紧密排列的
            // 可以简写  Method.class.getSuperclass() 这个类的 artMethod 就是artMethod地址。哈哈
            methodSize = (int) (method2Addr - method1Addr);
            if (methodSize < 0) {
                methodSize = -methodSize;
            }

            // 取方法1序列
            //init methodIndexOffset
            int method1MethodIndex = 0;
            Method[] methods = MethodSizeCase.class.getDeclaredMethods();
            for (int i = 0, size = methods.length; i < size; i++) {
                if (methods[i].equals(method1)) {
                    //why +1? Becase "FindVirtualMethodForVirtualOrInterface(method, sizeof(void*))" has the offset of sizeof(void*)
                    method1MethodIndex = i + 1;
                    break;
                }
            }
            //这是什么逻辑？
            //https://github.com/SeeYouOneDayDay/LearnEpic.git
            //ArtMethod.java    public ArtMethod backup()
            // 偏移四个单位验证，最终确认偏移值？
            // 这个需要区分art模式、dalvik模式和32位、64位？
            for (int i = 1, size = methodSize / 4; i < size; i++) {
                // 为什么偏移值需要*4？
                int value1 = UnsafeProxy.getIntVolatile(method1Addr + i * 4);
                int value2 = UnsafeProxy.getIntVolatile(method2Addr + i * 4);
                int value3 = UnsafeProxy.getIntVolatile(method3Addr + i * 4);
                // 根据方法序列来验证
                if (value1 == method1MethodIndex
                        && value2 == method1MethodIndex + 1
                        && value3 == method1MethodIndex + 2) {
                    methodIndexOffset = i * 4;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int methodSize() throws Exception {
        return methodSize;
    }

    @Override
    public int methodIndexOffset() throws Exception {
        return methodIndexOffset;
    }

}
