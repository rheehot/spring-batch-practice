package dev.hongsii.springbatchpractice.job;

import dev.hongsii.springbatchpractice.domain.Account;
import dev.hongsii.springbatchpractice.domain.AccountHistory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@Configuration
public class AccountMigrationJobConfiguration {

    private static final String JOB_NAME = "AccountMigrationJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private int chunkSize;

    public AccountMigrationJobConfiguration(JobBuilderFactory jobBuilderFactory,
                                            StepBuilderFactory stepBuilderFactory,
                                            EntityManagerFactory entityManagerFactory,
                                            @Value("${chunkSize:1000}") int chunkSize) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.entityManagerFactory = entityManagerFactory;
        this.chunkSize = chunkSize;
    }

    @Bean(JOB_NAME)
    public Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(step())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean(JOB_NAME + "Step")
    public Step step() {
        return stepBuilderFactory.get(JOB_NAME + "Step")
                .<Account, AccountHistory>chunk(chunkSize)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean(JOB_NAME + "Reader")
    public JpaPagingItemReader<Account> reader() {
        return new JpaPagingItemReaderBuilder<Account>()
                .name(JOB_NAME + "Reader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString("SELECT a FROM Account a")
                .build();
    }

    public ItemProcessor<Account, AccountHistory> processor() {
        return AccountHistory::new;
    }

    @Bean(JOB_NAME + "Writer")
    public JpaItemWriter<AccountHistory> writer() {
        return new JpaItemWriterBuilder<AccountHistory>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
