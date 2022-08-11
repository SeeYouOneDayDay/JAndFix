package com.tmall.wireless.jandfix;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by jingchaoqinjc on 17/5/15.
 */
public class MethodReplaceDalvik4_0 implements IMethodReplace {

    private final static int DIRECT_METHOD_OFFSET = 25;
    private final static int VIRTUAL_METHOD_OFFSET = 27;
    private final static int METHOD_SIZE_BYTE = 44;
    private final static int METHOD_INDEX_OFFSET = 2;

    static Field slotField;

    static {
        try {
            slotField = Method.class.getDeclaredField("slot");
            slotField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //android 4替换使用方法
    // 1. 获取替换方法、目标方法的虚地址
    // 1.1 偏移值计算,依赖访问符(final、static、private为直接访问，否则间接访问)
    // 直接访问,offset：25(直接访问方法偏移值) *4
    // 间接访问,offset：27(间接访问方法偏移值) *4
    // 1.2 通过Unsafe.getIntVolatile(method对象，offset)
    // 2. 计算真正地址
    // 2.1. 获取method的变量slot值
    // 2.2 真地址= 虚地址(1获取的值)+ slot值*44(方法字节数)
    // 3. 替换方法
    @Override
    public void replace(Method src, Method dest) {
        try {
            Class classSrc = src.getDeclaringClass();
            Class classDest = dest.getDeclaringClass();
            long virtualMethodSrcAddr = 0;
            long virtualMethodDestAddr = 0;

            if (isDirect(src)) {
                virtualMethodSrcAddr = UnsafeProxy.getIntVolatile(classSrc, DIRECT_METHOD_OFFSET * 4);
                virtualMethodDestAddr = UnsafeProxy.getIntVolatile(classDest, DIRECT_METHOD_OFFSET * 4);
            } else {
                virtualMethodSrcAddr = UnsafeProxy.getIntVolatile(classSrc, VIRTUAL_METHOD_OFFSET * 4);
                virtualMethodDestAddr = UnsafeProxy.getIntVolatile(classDest, VIRTUAL_METHOD_OFFSET * 4);
            }

            //slot is methodIndex in action
            int slotSrc = ((Integer) slotField.get(src)).intValue();
            int slotDest = ((Integer) slotField.get(dest)).intValue();

            replaceReal(virtualMethodSrcAddr + slotSrc * METHOD_SIZE_BYTE, virtualMethodDestAddr + slotDest * METHOD_SIZE_BYTE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void replaceReal(long src, long dest) throws Exception {
        //why 1? index 0 is declaring_class, declaring_class need not replace.
        for (int i = 1, size = METHOD_SIZE_BYTE / 4; i < size; i++) {
            if (i == METHOD_INDEX_OFFSET) {
                int destValue = UnsafeProxy.getIntVolatile(dest + i * 4);
                int srcValue = UnsafeProxy.getIntVolatile(src + i * 4);
                //keep methodIndex
                int value = (srcValue & 0xFFFF000) | (destValue & 0x0000FFFF);
                UnsafeProxy.putIntVolatile(src + i * 4, value);
            } else {
                int value = UnsafeProxy.getIntVolatile(dest + i * 4);
                UnsafeProxy.putIntVolatile(src + i * 4, value);
            }
        }
    }

    private boolean isDirect(Method method) {
        int modifier = method.getModifiers();
        return Modifier.isFinal(modifier) || Modifier.isStatic(modifier)
                || Modifier.isPrivate(modifier);
    }
}
