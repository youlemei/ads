package com.lwz.ads.mapper;

import com.lwz.ads.mapper.bean.CountSum;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.List;

/**
 * @author liweizhou 2020/2/4
 */
public class ConvertRecordMapperTest {
    private static ConvertRecordMapper mapper;

    @BeforeClass
    public static void setUpMybatisDatabase() {
        SqlSessionFactory builder = new SqlSessionFactoryBuilder().build(ConvertRecordMapperTest.class.getClassLoader().getResourceAsStream("mybatisTestConfiguration/ConvertRecordMapperTestConfiguration.xml"));
        //you can use builder.openSession(false) to not commit to database
        mapper = builder.getConfiguration().getMapper(ConvertRecordMapper.class, builder.openSession(true));
    }

    @Test
    public void testCountSrcConvertSum() throws FileNotFoundException {
        List<CountSum> countSums = mapper.countSrcConvertSum(
                LocalDate.of(2020, 02, 03).atStartOfDay(),
                LocalDate.of(2020, 02, 04).atStartOfDay()
        );
    }

    @Test
    public void testCountConvertSum() throws FileNotFoundException {
        List<CountSum> countSums = mapper.countConvertSum(
                LocalDate.of(2020, 02, 03).atStartOfDay(),
                LocalDate.of(2020, 02, 04).atStartOfDay()
        );
    }
}
