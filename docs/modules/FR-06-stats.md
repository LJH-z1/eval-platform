# FR-06 评分一致性分析(Fleiss Kappa)— TODO

> **负责人:宋子翔**
> **关联分支**:`feature/score`
> **预计工作量**:5 人日
> **对应评分层**:挑战层

## 一、任务清单

### 1.1 FleissKappaService 算法实现(2 天)

> **算法参考测试计划 §6.12 与 §12.4,4 个测试用例必须全过**

```java
public double calculate(List<List<Integer>> scores, int kCategories) {
    int N = scores.size();       // 评分对象数(回答数)
    int n = scores.get(0).size(); // 评分员数
    int k = kCategories;          // 类别数(5 个分值)

    int[][] count = new int[N][k];
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < n; j++) {
            int category = scores.get(i).get(j) - 1; // 1-5 → 0-4
            count[i][category]++;
        }
    }

    double sumP = 0;
    for (int i = 0; i < N; i++) {
        double pi = 0;
        for (int j = 0; j < k; j++) pi += Math.pow(count[i][j], 2);
        pi = (pi - n) / (double)(n * (n - 1));
        sumP += pi;
    }
    double P = sumP / N;

    double[] pj = new double[k];
    for (int j = 0; j < k; j++) {
        for (int i = 0; i < N; i++) pj[j] += count[i][j];
        pj[j] /= (double)(N * n);
    }

    double Pe = 0;
    for (int j = 0; j < k; j++) Pe += Math.pow(pj[j], 2);

    return (P - Pe) / (1 - Pe);
}
```

### 1.2 业务接口(1 天)

- [ ] `FleissKappaService.getKappa(evaluationId)` — 收集 score 数据,自动调用 calculate
- [ ] `FleissKappaService.getControversialItems` — 同问题标准差 > 1.5
- [ ] `FleissKappaService.getScorerRanking` — 平均分、覆盖率、被申诉次数
- [ ] `FleissKappaService.getModelRanking` — 各维度平均分 + 加权总分(准确*0.4 + 相关*0.3 + 流畅*0.2 + 安全*0.1)

### 1.3 缓存(0.5 天)

- [ ] Redis 缓存 5 分钟,key = `kappa:eval:{id}`
- [ ] 命中率 ≥ 80%(用 Caffeine 做二级缓存)

### 1.4 Controller(0.5 天)

- [ ] 完善 `StatsController`,加 Swagger 注解

### 1.5 单元测试(1 天) — 覆盖 TC-06-001 ~ 006 + TC-KAPPA-001 ~ 004

> **关键测试数据**(必须全过):

| 用例 | 期望 | 说明 |
|---|---|---|
| TC-KAPPA-001 完美一致 | 1.0 | 3 评分员,5 对象,5 类别,完全相同 |
| TC-KAPPA-002 随机一致 | ≈ 0 | 3 评分员,5 对象,5 类别,完全随机 |
| TC-KAPPA-003 Fleiss 1971 教科书 | 0.591 | 6 评分员,5 对象 |
| TC-KAPPA-004 实测误差 | < 0.001 | 与 Python 参考实现对比 |

**TC-KAPPA-003 数据(Fleiss 1971 原始论文)**:
```
对象  | 类别1 | 类别2 | 类别3 | 类别4 | 类别5
1     | 0     | 0     | 0     | 0     | 6
2     | 0     | 0     | 2     | 4     | 0
3     | 0     | 1     | 1     | 2     | 2
4     | 1     | 0     | 2     | 3     | 0
5     | 0     | 4     | 0     | 1     | 1
```

## 二、业务规则(对齐需求规格说明书 §3.6.4)

- 至少 3 名评分员、每回答 ≥ 2 个评分,否则提示"评分员不足"
- Kappa 计算每 5 分钟缓存一次
- 解读:< 0 差 / 0-0.2 轻微 / 0.2-0.4 一般 / 0.4-0.6 中等 / 0.6-0.8 良好 / > 0.8 优秀

## 三、参考资料

- 完整参考:`eval-platform-reference/backend/src/main/java/com/mavis/evalplatform/stats/`
- 需求:§3.6
- 架构:§4.2.6
- 测试:§6.6 + §6.12(核心算法)
- Fleiss 1971 原始论文:M.L. Fleiss, "Measuring nominal scale agreement among many raters", Psychological Bulletin, 1971
