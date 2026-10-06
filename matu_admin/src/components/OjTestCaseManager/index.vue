<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createOjTestCaseApi, getOjTestCaseListApi } from '@/api'
import type { CreateOjTestCaseRequest, OjTestCaseVO } from '@/api/types'
import './index.scss'

const props = defineProps<{
  problemId?: string
}>()

const loading = ref(false)
const submitLoading = ref(false)
const testCaseList = ref<OjTestCaseVO[]>([])
const testCaseForm = reactive<CreateOjTestCaseRequest>({
  caseNo: 1,
  input: '',
  expectedOutput: '',
  isSample: 0,
  scoreWeight: 1,
  isHidden: 0,
})

const yesNoOptions = [
  { label: '否', value: 0 },
  { label: '是', value: 1 },
]

const formatDateTime = (value?: string) => (!value ? '-' : value.replace('T', ' '))

const resetForm = () => {
  testCaseForm.caseNo = (testCaseList.value[testCaseList.value.length - 1]?.caseNo || 0) + 1
  testCaseForm.input = ''
  testCaseForm.expectedOutput = ''
  testCaseForm.isSample = 0
  testCaseForm.scoreWeight = 1
  testCaseForm.isHidden = 0
}

const fetchList = async () => {
  if (!props.problemId) {
    testCaseList.value = []
    return
  }

  loading.value = true
  try {
    const res = await getOjTestCaseListApi(props.problemId)
    testCaseList.value = res.data || []
    resetForm()
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!props.problemId) return

  submitLoading.value = true
  try {
    await createOjTestCaseApi(props.problemId, { ...testCaseForm })
    ElMessage.success('测试用例新增成功')
    await fetchList()
  } finally {
    submitLoading.value = false
  }
}

watch(
  () => props.problemId,
  () => {
    void fetchList()
  },
  { immediate: true },
)
</script>

<template>
  <div class="oj-testcase-manager">
    <div class="oj-testcase-manager__tips">
      <span>新增 OJ 题目后，请继续补充测试用例。</span>
      <span>调用接口：`POST /oj/classes/problems/{problemId}/test-cases`</span>
    </div>

    <div class="oj-testcase-manager__grid">
      <el-input-number v-model="testCaseForm.caseNo" :min="1" controls-position="right" />
      <el-input-number
        v-model="testCaseForm.scoreWeight"
        :min="0"
        :step="0.5"
        controls-position="right"
      />
      <el-select v-model="testCaseForm.isSample">
        <el-option
          v-for="item in yesNoOptions"
          :key="`sample-${item.value}`"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-select v-model="testCaseForm.isHidden">
        <el-option
          v-for="item in yesNoOptions"
          :key="`hidden-${item.value}`"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </div>

    <div class="oj-testcase-manager__grid">
      <el-input
        v-model="testCaseForm.input"
        type="textarea"
        :rows="4"
        placeholder="input：输入数据，对应 oj_test_cases.input"
      />
      <el-input
        v-model="testCaseForm.expectedOutput"
        type="textarea"
        :rows="4"
        placeholder="expected_output：期望输出，对应 oj_test_cases.expected_output"
      />
    </div>

    <div class="oj-testcase-manager__toolbar">
      <el-button
        type="primary"
        :loading="submitLoading"
        :disabled="!problemId"
        @click="handleCreate"
        >新增测试用例</el-button
      >
    </div>

    <el-table :data="testCaseList" v-loading="loading" width="100%">
      <el-table-column prop="id" label="id" width="90" />
      <el-table-column prop="problemId" label="problem_id" width="110" />
      <el-table-column prop="caseNo" label="case_no" width="100" />
      <el-table-column prop="input" label="input" min-width="180" show-overflow-tooltip />
      <el-table-column
        prop="expectedOutput"
        label="expected_output"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column label="is_sample" width="100">
        <template #default="scope">{{ scope.row.isSample ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="scoreWeight" label="score_weight" width="120" />
      <el-table-column label="is_hidden" width="100">
        <template #default="scope">{{ scope.row.isHidden ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="created_at" min-width="180">
        <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>
