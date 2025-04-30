package com.test;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.BeanCopier;
import com.taowater.taol.core.convert.ConvertUtil;
import com.taowater.taol.core.util.CollUtil;
import com.test.bean.Bean1;
import com.test.bean.Bean2;
import com.test.bean.BeanMapper;
import io.vavr.Function2;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.function.BiConsumer;

public class ConvertTest {

    public static void main(String[] args) {


        Bean1 bean1 = getBean1();


        Bean2 bean2 = new Bean2();

        System.out.println("taol:" + ConvertUtil.convert(bean1, Bean2.class));
        Bean2 hutoolBean2 = BeanUtil.copyProperties(bean1, Bean2.class);
        System.out.println("hutool:" + hutoolBean2);
        BeanUtils.copyProperties(bean1, bean2);
        System.out.println("spring:" + bean2);
        Bean2 bean3 = new Bean2();
        BeanCopier.create(bean1, bean3, null).copy();
        System.out.println("cglib:" + bean3);
        System.out.println("mapstruct:" + BeanMapper.INSTANCE.to(bean1));
        Function2<String, BiConsumer<Object, Object>, Void> testFun = (name, fun) -> {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++) {
                fun.accept(bean1, bean2);
            }
            long end = System.currentTimeMillis();
            System.out.printf("%10s:%dms\n", name, (end - start));
            return null;
        };

        testFun.apply("taol", ConvertUtil::copy);
        testFun.apply("hutool", BeanUtil::copyProperties);
        testFun.apply("spring", BeanUtils::copyProperties);
        testFun.apply("cglib", (b1, b2) -> BeanCopier.create(b1, b2, null).copy());
        testFun.apply("mapstruct", (b1, b2) -> BeanMapper.INSTANCE.to((Bean1) b1));
    }

    private static Bean1 getBean1() {
        Bean1 bean1 = new Bean1();
        bean1.setId(1L);
        bean1.setName("jack");
        bean1.setAge(123);
        bean1.setList(CollUtil.list(1, 4, 6, 9));
        bean1.setList2(new List[]{CollUtil.list(1), CollUtil.list(5), CollUtil.list(5)});
        bean1.setField1("field1");
        bean1.setField2("field2");
        bean1.setField3("field3");
        bean1.setField4("field4");
        bean1.setField5("field5");
        bean1.setField6("field6");
        bean1.setField7("field7");
        bean1.setField8("field8");
        bean1.setField9("field9");
        bean1.setField10("field10");
        bean1.setField11("field11");
        bean1.setField12("field12");
        bean1.setField13("field13");
        bean1.setField14("field14");
        bean1.setField15("field15");
        bean1.setField16("field16");
        bean1.setField17("field17");
        bean1.setField18("field18");
        bean1.setField19("field19");
        bean1.setField20("field20");
        bean1.setField21("field21");
        bean1.setField22("field22");
        bean1.setField23("field23");
        bean1.setField24("field24");
        bean1.setField25("field25");
        bean1.setField26("field26");
        bean1.setField27("field27");
        bean1.setField28("field28");
        bean1.setField29("field29");
        bean1.setField30("field30");
        bean1.setField31("field31");
        bean1.setField32("field32");
        bean1.setField33("field33");
        bean1.setField34("field34");
        bean1.setField35("field35");
        bean1.setField36("field36");
        bean1.setField37("field37");
        bean1.setField38("field38");
        bean1.setField39("field39");
        bean1.setField40("field40");
        bean1.setField41("field41");
        bean1.setField42("field42");
        bean1.setField43("field43");
        bean1.setField44("field44");
        bean1.setField45("field45");
        bean1.setField46("field46");
        bean1.setField47("field47");
        bean1.setField48("field48");
        bean1.setField49("field49");
        bean1.setField50("field50");
        bean1.setField51("field51");
        bean1.setField52("field52");
        bean1.setField53("field53");
        bean1.setField54("field54");
        bean1.setField55("field55");
        bean1.setField56("field56");
        bean1.setField57("field57");
        bean1.setField58("field58");
        bean1.setField59("field59");
        bean1.setField60("field60");
        bean1.setField61("field61");
        bean1.setField62("field62");
        bean1.setField63("field63");
        bean1.setField64("field64");
        bean1.setField65("field65");
        bean1.setField66("field66");
        bean1.setField67("field67");
        bean1.setField68("field68");
        bean1.setField69("field69");
        bean1.setField70("field70");
        bean1.setField71("field71");
        bean1.setField72("field72");
        bean1.setField73("field73");
        bean1.setField74("field74");
        bean1.setField75("field75");
        bean1.setField76("field76");
        bean1.setField77("field77");
        bean1.setField78("field78");
        bean1.setField79("field79");
        bean1.setField80("field80");
        bean1.setField81("field81");
        bean1.setField82("field82");
        bean1.setField83("field83");
        bean1.setField84("field84");
        bean1.setField85("field85");
        bean1.setField86("field86");
        bean1.setField87("field87");
        bean1.setField88("field88");
        bean1.setField89("field89");
        bean1.setField90("field90");
        bean1.setField91("field91");
        bean1.setField92("field92");
        bean1.setField93("field93");
        bean1.setField94("field94");
        bean1.setField95("field95");
        bean1.setField96("field96");
        bean1.setField97("field97");
        bean1.setField98("field98");
        bean1.setField99("field99");
        bean1.setField100("field100");
        return bean1;
    }
}
