package io.hqwu.commons.utils;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for TransactionalTask
 *
 * @author taige
 * @since 2026-01-28
 */
class TransactionalTaskTest {

    @Test
    void testPrepareTasks_ReturnsTaskList() {
        // Given
        List<String> expectedTasks = Arrays.asList("task1", "task2", "task3");
        TransactionalTask<String> task = new TestTransactionalTask(expectedTasks);

        // When
        List<String> actualTasks = task.prepareTasks();

        // Then
        assertThat(actualTasks).isEqualTo(expectedTasks);
        assertThat(actualTasks).hasSize(3);
    }

    @Test
    void testPrepareTasks_ReturnsEmptyList() {
        // Given
        TransactionalTask<String> task = new TestTransactionalTask(Collections.emptyList());

        // When
        List<String> actualTasks = task.prepareTasks();

        // Then
        assertThat(actualTasks).isEmpty();
    }

    @Test
    void testRunTask_ExecutesSuccessfully() {
        // Given
        List<String> executedTasks = new ArrayList<>();
        TransactionalTask<String> task = new TestTransactionalTask(Collections.emptyList()) {
            @Override
            @Transactional
            public void runTask(String taskItem) {
                executedTasks.add(taskItem);
            }
        };

        // When
        task.runTask("testTask");

        // Then
        assertThat(executedTasks).containsExactly("testTask");
    }

    @Test
    void testRunTask_ThrowsException() {
        // Given
        TransactionalTask<String> task = new TestTransactionalTask(Collections.emptyList()) {
            @Override
            @Transactional
            public void runTask(String taskItem) {
                throw new RuntimeException("Task execution failed");
            }
        };

        // When & Then
        assertThatThrownBy(() -> task.runTask("failTask"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Task execution failed");
    }

    @Test
    void testOnException_DefaultImplementation() {
        // Given
        TransactionalTask<String> task = new TestTransactionalTask(Collections.emptyList());
        RuntimeException exception = new RuntimeException("Test exception");

        // When - default implementation should not throw
        task.onException("testTask", exception);

        // Then - no exception should be thrown
        assertThat(exception).isNotNull();
    }

    @Test
    void testOnException_CustomImplementation() {
        // Given
        List<String> failedTasks = new ArrayList<>();
        List<RuntimeException> exceptions = new ArrayList<>();

        TransactionalTask<String> task = new TestTransactionalTask(Collections.emptyList()) {
            @Override
            public void onException(String taskItem, RuntimeException exception) {
                failedTasks.add(taskItem);
                exceptions.add(exception);
            }
        };

        RuntimeException testException = new RuntimeException("Custom exception");

        // When
        task.onException("failedTask", testException);

        // Then
        assertThat(failedTasks).containsExactly("failedTask");
        assertThat(exceptions).containsExactly(testException);
    }

    @Test
    void testOnException_WithNullTask() {
        // Given
        List<String> failedTasks = new ArrayList<>();
        TransactionalTask<String> task = new TestTransactionalTask(Collections.emptyList()) {
            @Override
            public void onException(String taskItem, RuntimeException exception) {
                failedTasks.add(taskItem);
            }
        };

        // When
        task.onException(null, new RuntimeException("Exception"));

        // Then
        assertThat(failedTasks).containsExactly((String) null);
    }

    /**
     * Test implementation of TransactionalTask for testing purposes
     */
    private static class TestTransactionalTask implements TransactionalTask<String> {
        private final List<String> tasks;

        public TestTransactionalTask(List<String> tasks) {
            this.tasks = tasks;
        }

        @Override
        public List<String> prepareTasks() {
            return tasks;
        }

        @Override
        @Transactional
        public void runTask(String task) {
            // Default implementation does nothing
        }
    }
}
