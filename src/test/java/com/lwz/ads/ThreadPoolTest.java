package com.lwz.ads;

import com.lwz.ads.util.SmartRejectedExecutionHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author liweizhou 2020/11/24
 */
public class ThreadPoolTest {

    public static void main(String[] args) throws Exception {

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 10, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new SmartRejectedExecutionHandler());

        for (int i = 0; i < 4; i++) {
            threadPoolExecutor.execute(()->{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        for (int i = 0; i < 5; i++) {
            threadPoolExecutor.execute(()->{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        for (int i = 0; i < 12; i++) {
            threadPoolExecutor.execute(()->{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }


    }

}
