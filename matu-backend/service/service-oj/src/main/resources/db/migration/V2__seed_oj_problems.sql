-- Seed: 10 starter OJ problems (5 beginner + 5 intermediate).
--
-- Ids are fixed and small (problems 1001-1010, contents 2001-2010, cases
-- 3001-3030) so this migration is deterministic and replayable. They cannot
-- collide with MyBatis-Plus ASSIGN_ID snowflake ids (19-digit, ~2.0e18).
--
-- text columns use \n for line breaks; the starter_code_json JSON column needs
-- escaped newlines, so it is written with \\n (MySQL turns \\ into one
-- backslash, leaving the JSON parser a valid \n escape inside each string).
--
-- category_id 1 matches the existing problem (A1). difficulty 1=beginner,
-- 2=intermediate. status 1=published.

-- ---------------------------------------------------------------- problems
INSERT INTO oj_problems
  (id, problem_no, title, description, input_format, output_format, sample_input, sample_output, hint, difficulty, category_id, time_limit, memory_limit, submit_count, accept_count, accept_rate, status)
VALUES
  (1001, 'A2', '数组求和', '给定 n 个整数，求它们的和。', '第一行一个整数 n（1 ≤ n ≤ 1000），第二行 n 个整数。', '输出这 n 个整数的和。', '5\n1 2 3 4 5', '15', '使用循环累加每个数即可。', 1, 1, 1000, 256, 0, 0, 0.00, 1),
  (1002, 'A3', '数组最大值', '给定 n 个整数，输出其中的最大值。', '第一行一个整数 n（1 ≤ n ≤ 1000），第二行 n 个整数。', '输出这 n 个整数中的最大值。', '5\n3 9 2 7 4', '9', '依次比较，维护当前最大值。', 1, 1, 1000, 256, 0, 0, 0.00, 1),
  (1003, 'A4', '字符串反转', '给定一个由小写字母组成的字符串，输出它的反转。', '一行一个字符串 s（1 ≤ |s| ≤ 1000），不含空格。', '输出反转后的字符串。', 'hello', 'olleh', '可以倒序遍历字符串依次输出。', 1, 1, 1000, 256, 0, 0, 0.00, 1),
  (1004, 'A5', '判断质数', '给定一个正整数 n，判断它是否为质数。', '一行一个整数 n（1 ≤ n ≤ 10^9）。', '如果 n 是质数输出 YES，否则输出 NO。', '7', 'YES', '只需试除到 sqrt(n) 即可。', 1, 1, 1000, 256, 0, 0, 0.00, 1),
  (1005, 'A6', '数字各位之和', '给定一个非负整数 n，求它十进制各位数字之和。', '一行一个整数 n（0 ≤ n ≤ 10^18）。', '输出各位数字之和。', '123', '6', '反复对 10 取余并除以 10。', 1, 1, 1000, 256, 0, 0, 0.00, 1),
  (1006, 'B1', '冒泡排序', '给定 n 个整数，将它们按升序排序后输出。', '第一行一个整数 n（1 ≤ n ≤ 1000），第二行 n 个整数。', '输出排序后的 n 个整数，用空格分隔。', '5\n5 3 8 1 2', '1 2 3 5 8', '相邻元素两两比较并交换。', 2, 1, 1000, 256, 0, 0, 0.00, 1),
  (1007, 'B2', '二分查找', '给定一个升序排列的整数数组和一个目标值 x，输出 x 在数组中的下标（从 0 开始）；若不存在输出 -1。', '第一行一个整数 n（1 ≤ n ≤ 10^5），第二行 n 个升序整数，第三行一个整数 x。', '输出 x 的下标，不存在则输出 -1。', '5\n1 3 5 7 9\n5', '2', '在有序数组中每次将查找区间折半。', 2, 1, 1000, 256, 0, 0, 0.00, 1),
  (1008, 'B3', '斐波那契数列', '斐波那契数列定义 F1 = 1，F2 = 1，Fn = F(n-1) + F(n-2)。给定 n，求 Fn。', '一行一个整数 n（1 ≤ n ≤ 40）。', '输出 Fn。', '6', '8', '用两个变量递推即可。', 2, 1, 1000, 256, 0, 0, 0.00, 1),
  (1009, 'B4', '回文判断', '给定一个由小写字母组成的字符串，判断它是否为回文串。', '一行一个字符串 s（1 ≤ |s| ≤ 1000），不含空格。', '如果 s 是回文串输出 YES，否则输出 NO。', 'level', 'YES', '首尾向中间依次比较。', 2, 1, 1000, 256, 0, 0, 0.00, 1),
  (1010, 'B5', '统计元音字母', '给定一个由小写字母组成的字符串，统计其中元音字母（a、e、i、o、u）的个数。', '一行一个字符串 s（1 ≤ |s| ≤ 1000），不含空格。', '输出元音字母的个数。', 'hello', '2', '遍历字符串逐个判断字符是否为元音。', 2, 1, 1000, 256, 0, 0, 0.00, 1);

-- ---------------------------------------------------------------- contents
INSERT INTO oj_problem_contents
  (id, problem_id, content_md, content_html, starter_code_json, solution_json, tags_json, version)
VALUES
  (2001, 1001, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1),
  (2002, 1002, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1),
  (2003, 1003, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1),
  (2004, 1004, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1),
  (2005, 1005, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1),
  (2006, 1006, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1),
  (2007, 1007, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1),
  (2008, 1008, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1),
  (2009, 1009, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1),
  (2010, 1010, NULL, NULL, '{"cpp": "#include <bits/stdc++.h>\\nusing namespace std;\\n\\nint main() {\\n    // 在此处编写你的代码\\n    return 0;\\n}", "java": "import java.util.*;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        // 在此处编写你的代码\\n    }\\n}", "python": "# 在此处编写你的代码\\n"}', NULL, '[]', 1);

-- ---------------------------------------------------------------- test cases
INSERT INTO oj_test_cases
  (id, problem_id, case_no, input, expected_output, is_sample, score_weight, is_hidden)
VALUES
  (3001, 1001, 1, '5\n1 2 3 4 5', '15', 1, 1.00, 0),
  (3002, 1001, 2, '1\n42', '42', 0, 1.00, 1),
  (3003, 1001, 3, '4\n-1 -2 -3 -4', '-10', 0, 1.00, 1),

  (3004, 1002, 1, '5\n3 9 2 7 4', '9', 1, 1.00, 0),
  (3005, 1002, 2, '3\n-5 -2 -9', '-2', 0, 1.00, 1),
  (3006, 1002, 3, '1\n100', '100', 0, 1.00, 1),

  (3007, 1003, 1, 'hello', 'olleh', 1, 1.00, 0),
  (3008, 1003, 2, 'abcde', 'edcba', 0, 1.00, 1),
  (3009, 1003, 3, 'a', 'a', 0, 1.00, 1),

  (3010, 1004, 1, '7', 'YES', 1, 1.00, 0),
  (3011, 1004, 2, '1', 'NO', 0, 1.00, 1),
  (3012, 1004, 3, '9', 'NO', 0, 1.00, 1),

  (3013, 1005, 1, '123', '6', 1, 1.00, 0),
  (3014, 1005, 2, '0', '0', 0, 1.00, 1),
  (3015, 1005, 3, '999', '27', 0, 1.00, 1),

  (3016, 1006, 1, '5\n5 3 8 1 2', '1 2 3 5 8', 1, 1.00, 0),
  (3017, 1006, 2, '3\n-1 0 1', '-1 0 1', 0, 1.00, 1),
  (3018, 1006, 3, '4\n4 4 3 3', '3 3 4 4', 0, 1.00, 1),

  (3019, 1007, 1, '5\n1 3 5 7 9\n5', '2', 1, 1.00, 0),
  (3020, 1007, 2, '4\n2 4 6 8\n8', '3', 0, 1.00, 1),
  (3021, 1007, 3, '3\n1 2 3\n10', '-1', 0, 1.00, 1),

  (3022, 1008, 1, '6', '8', 1, 1.00, 0),
  (3023, 1008, 2, '1', '1', 0, 1.00, 1),
  (3024, 1008, 3, '10', '55', 0, 1.00, 1),

  (3025, 1009, 1, 'level', 'YES', 1, 1.00, 0),
  (3026, 1009, 2, 'hello', 'NO', 0, 1.00, 1),
  (3027, 1009, 3, 'abccba', 'YES', 0, 1.00, 1),

  (3028, 1010, 1, 'hello', '2', 1, 1.00, 0),
  (3029, 1010, 2, 'aeiou', '5', 0, 1.00, 1),
  (3030, 1010, 3, 'xyz', '0', 0, 1.00, 1);
