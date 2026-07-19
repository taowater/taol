package com.taowater.core.reflect;

import com.taowater.taol.core.convert.ConvertUtil;
import com.taowater.taol.core.convert.GetSetHelper;
import com.taowater.taol.core.reflect.ClassUtil;
import com.taowater.taol.core.reflect.MethodHandleHelper;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MethodHandleHelper} 及依赖它的 {@link ClassUtil}/{@link GetSetHelper} 回归。
 * <p>
 * 用例刻意覆盖「跨包 + 包级私有类 + 私有成员」：Helper 位于
 * {@code com.taowater.taol.core.reflect}，夹具在 {@code com.taowater.core.reflect}，
 * 验证 JDK16+ 强封装下静态初始化不会因 {@code IMPL_LOOKUP.setAccessible} 失败而崩，
 * 并能通过 {@code privateLookupIn} 等路径完成特权 Lookup。
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
