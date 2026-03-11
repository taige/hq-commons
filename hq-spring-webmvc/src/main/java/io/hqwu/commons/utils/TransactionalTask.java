package io.hqwu.commons.utils;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 事务任务执行接口
 * <p>
 * 该接口定义了一个通用的事务任务批处理框架，用于在事务上下文中批量执行任务。
 * 接口采用模板方法模式，将任务的准备、执行和异常处理分离，确保每个任务在独立的事务中执行。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>准备待执行的任务列表</li>
 *   <li>在独立事务中执行单个任务</li>
 *   <li>提供任务执行失败时的异常处理钩子</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>批量数据处理，每条数据的处理需要独立的事务</li>
 *   <li>需要保证单个任务失败不影响其他任务执行的场景</li>
 *   <li>需要对任务执行异常进行自定义处理的场景</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * public class OrderTransactionalTask implements TransactionalTask<Order> {
 *     @Override
 *     public List<Order> prepareTasks() {
 *         return orderRepository.findPendingOrders();
 *     }
 *
 *     @Override
 *     @Transactional
 *     public void runTask(Order order) {
 *         // 在事务中处理订单
 *         orderService.processOrder(order);
 *     }
 *
 *     @Override
 *     public void onException(Order order, RuntimeException exception) {
 *         // 记录失败的订单
 *         logger.error("处理订单失败: {}", order.getId(), exception);
 *     }
 * }
 * }</pre>
 *
 * @param <T> 任务对象的类型
 * @author taige (Wu, Hongqiang)
 * @see Transactional
 * @since 2021-04-26
 * @deprecated 接口上的 @Transactional 注解在某些代理模式下可能失效，且容易导致自调用事务失效的问题。
 *             建议直接在具体业务类中使用 @Transactional 注解，或使用编程式事务 TransactionTemplate。
 */
@Deprecated
public interface TransactionalTask<T> {

    /**
     * 准备待执行的任务列表
     * <p>
     * 该方法用于收集和准备所有需要在事务中执行的任务。
     * 每个任务将在独立的事务中通过 {@link #runTask(Object)} 方法执行。
     * </p>
     *
     * @return 待执行的任务列表，不能为 null
     */
    List<T> prepareTasks();

    /**
     * 在事务中执行单个任务
     * <p>
     * 该方法会在独立的事务上下文中执行单个任务。
     * 如果任务执行失败抛出异常，事务将回滚，并触发 {@link #onException(Object, RuntimeException)} 回调。
     * </p>
     *
     * @param task 要执行的任务对象
     */
    @Transactional
    void runTask(T task);

    /**
     * 任务执行异常处理钩子方法
     * <p>
     * 当 {@link #runTask(Object)} 执行过程中抛出 RuntimeException 时，该方法会被调用。
     * 默认实现为空，子类可以重写此方法来实现自定义的异常处理逻辑，如记录日志、发送告警等。
     * </p>
     *
     * @param task      执行失败的任务对象
     * @param exception 任务执行过程中抛出的运行时异常
     */
    default void onException(T task, RuntimeException exception) {

    }

}
