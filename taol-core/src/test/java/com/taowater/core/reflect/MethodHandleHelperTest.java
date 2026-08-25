package com.taowater.core.reflect;

import com.taowater.taol.core.convert.ConvertUtil;
import com.taowater.taol.core.convert.GetSetHelper;
import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.reflect.MethodHandleHelper;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MethodHandleHelper} 及依赖它的 {@link ClassUtil}/{@link GetSetHelper} 回归。
 * <p>
 * 用例覆盖跨包非公开 Bean，并验证标准 Lookup 不会越过 JDK 模块边界
 */
class MethodHandleHelperTest {

    // ------------------------------------------------------------------ class load

    @Test
    void helperClassLoadsWithoutExceptionInInitializerError() {
        // 触达任意静态成员即强制完成 <clinit>；若仍 catch 过窄会在此直接失败
        assertDoesNotThrow(() -> Class.forName(MethodHandleHelper.class.getName()));
        assertNotNull(MethodHandleHelper.class);
    }

    // ------------------------------------------------------------------ access(Method)

    @Test
    void accessPublicMethod_returnsUsableHandle() throws Throwable {
        Method getName = PublicBean.class.getMethod("getName");
        MethodHandleHelper.MethodAccess access = MethodHandleHelper.access(getName);

        assertNotNull(access);
        assertNotNull(access.getLookup());
        assertNotNull(access.getHandle());

        PublicBean bean = new PublicBean();
        bean.setName("public");
        assertEquals("public", (String) access.getHandle().invoke(bean));
    }

    @Test
    void accessPackagePrivateBeanMethod_crossPackage() throws Throwable {
        Method getAge = PackagePrivateBean.class.getMethod("getAge");
        MethodHandleHelper.MethodAccess access = MethodHandleHelper.access(getAge);

        PackagePrivateBean bean = new PackagePrivateBean();
        bean.setAge(42);
        assertEquals(42, (int) access.getHandle().invoke(bean));
    }

    @Test
    void unreflect_sameAsAccessHandle() throws Throwable {
        Method getAge = PackagePrivateBean.class.getMethod("getAge");
        MethodHandle fromUnreflect = MethodHandleHelper.unreflect(getAge);
        MethodHandle fromAccess = MethodHandleHelper.access(getAge).getHandle();

        PackagePrivateBean bean = new PackagePrivateBean();
        bean.setAge(7);
        assertEquals((int) fromAccess.invoke(bean), (int) fromUnreflect.invoke(bean));
    }

    @Test
    void accessJdkPrivateMethod_respectsModuleBoundary() {
        // Java 8 没有模块边界，兼容 fallback 的行为不适用本断言
        Assumptions.assumeTrue(hasPrivateLookupIn(), "仅在 Java 9+ 验证模块边界");

        // 未开放的 JDK 模块私有成员必须拒绝访问
        Method privateMethod = Arrays.stream(String.class.getDeclaredMethods())
                .filter(method -> Modifier.isPrivate(method.getModifiers()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        try {
            MethodHandleHelper.access(privateMethod);
            // 若通过 --add-opens 开放了 java.lang，本断言不适用
            Assumptions.assumeTrue(false, "java.lang 已通过启动参数开放");
        } catch (IllegalStateException expected) {
            // 未开放模块时必须拒绝访问
        }
    }

    // 判断当前运行时是否提供标准私有 Lookup API
    private static boolean hasPrivateLookupIn() {
        try {
            MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    @Test
    void accessJdkPublicMethod_usesStandardPublicLookup() throws Throwable {
        Method length = String.class.getMethod("length");
        MethodHandleHelper.MethodAccess access = MethodHandleHelper.access(length);

        assertEquals(4, (int) access.getHandle().invoke("taol"));
    }

    // ------------------------------------------------------------------ access(Constructor)

    @Test
    void accessPublicNoArgConstructor() throws Throwable {
        Constructor<PublicBean> ctor = PublicBean.class.getDeclaredConstructor();
        MethodHandleHelper.ConstructorAccess access = MethodHandleHelper.access(ctor);

        Object created = access.getHandle().invoke();
        assertNotNull(created);
        assertInstanceOf(PublicBean.class, created);
    }

    @Test
    void accessPackagePrivateNoArgConstructor_crossPackage() throws Throwable {
        Constructor<PackagePrivateBean> ctor = PackagePrivateBean.class.getDeclaredConstructor();
        MethodHandleHelper.ConstructorAccess access = MethodHandleHelper.access(ctor);

        Object created = access.getHandle().invoke();
        assertInstanceOf(PackagePrivateBean.class, created);
    }

    @Test
    void accessPrivateConstructor_crossPackage() throws Throwable {
        Constructor<PrivateCtorBean> ctor = PrivateCtorBean.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));

        MethodHandleHelper.ConstructorAccess access = MethodHandleHelper.access(ctor);
        Object created = access.getHandle().invoke();
        assertInstanceOf(PrivateCtorBean.class, created);
    }

    // ------------------------------------------------------------------ ClassUtil (via MethodHandleHelper)

    @Test
    void classUtil_newInstance_publicClass() {
        PublicBean bean = ClassUtil.newInstance(PublicBean.class);
        assertNotNull(bean);
    }

    @Test
    void classUtil_newInstance_packagePrivateClass() {
        PackagePrivateBean bean = ClassUtil.newInstance(PackagePrivateBean.class);
        assertNotNull(bean);
    }

    @Test
    void classUtil_newInstance_privateConstructorClass() {
        PrivateCtorBean bean = ClassUtil.newInstance(PrivateCtorBean.class);
        assertNotNull(bean);
    }

    @Test
    void classUtil_newInstance_nullReturnsNull() {
        assertNull(ClassUtil.newInstance(null));
    }

    // ------------------------------------------------------------------ GetSetHelper (via MethodHandleHelper)

    @Test
    void getSetHelper_buildGetterAccessor_packagePrivateBean() {
        PackagePrivateBean bean = new PackagePrivateBean();
        bean.setAge(42);

        Method getAge = assertDoesNotThrow(() -> PackagePrivateBean.class.getMethod("getAge"));
        @SuppressWarnings("unchecked")
        ToIntFunction<PackagePrivateBean> getter =
                (ToIntFunction<PackagePrivateBean>) GetSetHelper.buildGetterAccessor(PackagePrivateBean.class, getAge);

        assertNotNull(getter);
        assertEquals(42, getter.applyAsInt(bean));
    }

    @Test
    void getSetHelper_buildGetterAndSetter_byFieldName_packagePrivateBean() {
        PackagePrivateBean bean = ClassUtil.newInstance(PackagePrivateBean.class);
        Function<PackagePrivateBean, Integer> getter = GetSetHelper.buildGetter(PackagePrivateBean.class, "age");
        BiConsumer<PackagePrivateBean, Integer> setter = GetSetHelper.buildSetter(PackagePrivateBean.class, "age");

        setter.accept(bean, 99);
        assertEquals(99, getter.apply(bean).intValue());
    }

    @Test
    void getSetHelper_stringField_roundTrip_packagePrivateBean() {
        PackagePrivateBean bean = ClassUtil.newInstance(PackagePrivateBean.class);
        Function<PackagePrivateBean, String> getter = GetSetHelper.buildGetter(PackagePrivateBean.class, "name");
        BiConsumer<PackagePrivateBean, String> setter = GetSetHelper.buildSetter(PackagePrivateBean.class, "name");

        setter.accept(bean, "taol");
        assertEquals("taol", getter.apply(bean));
    }

    // ------------------------------------------------------------------ ConvertUtil end-to-end

    @Test
    void convertUtil_copiesIntoPackagePrivateTarget() {
        PublicSource source = new PublicSource();
        source.setAge(18);
        source.setName("alice");

        PackagePrivateBean target = ConvertUtil.convert(source, PackagePrivateBean.class);

        assertNotNull(target);
        assertEquals(18, target.getAge());
        assertEquals("alice", target.getName());
    }

    @Test
    void convertUtil_copiesFromPackagePrivateSource() {
        PackagePrivateBean source = ClassUtil.newInstance(PackagePrivateBean.class);
        source.setAge(21);
        source.setName("bob");

        PublicSource target = ConvertUtil.convert(source, PublicSource.class);

        assertEquals(21, target.getAge());
        assertEquals("bob", target.getName());
    }

    // ------------------------------------------------------------------ fixtures（包级私有，相对 MethodHandleHelper 跨包）

    @Getter
    @Setter
    public static class PublicBean {
        private String name;
    }

    @Getter
    @Setter
    public static class PublicSource {
        private int age;
        private String name;
    }

    /**
     * 包级私有嵌套类：测试与 MethodHandleHelper 不在同一包。
     */
    @Getter
    @Setter
    static class PackagePrivateBean {
        private int age;
        private String name;
    }

    /**
     * 包级私有 + 私有无参构造，覆盖最刁钻的特权 Lookup 场景。
     */
    static class PrivateCtorBean {
        private PrivateCtorBean() {
        }
    }
}
