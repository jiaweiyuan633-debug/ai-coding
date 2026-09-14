package com.aicoding.core.ai.mock;

/**
 * Mock 模式的演示页面内容（模拟 AI 生成的单文件 HTML）
 */
public class MockContents {

    private MockContents() {
    }

    public static String demoHtml(String title, String subtitle) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
                       background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                       min-height: 100vh; display: flex; align-items: center; justify-content: center; }
                .card { background: rgba(255,255,255,.95); border-radius: 24px; padding: 48px 56px;
                        box-shadow: 0 24px 80px rgba(0,0,0,.25); max-width: 640px; text-align: center; }
                h1 { font-size: 32px; color: #2d3748; margin-bottom: 12px; }
                p  { color: #718096; font-size: 16px; line-height: 1.8; margin-bottom: 28px; }
                .btn { display: inline-block; padding: 12px 36px; border: none; border-radius: 999px;
                       background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; font-size: 16px;
                       cursor: pointer; transition: transform .2s, box-shadow .2s; }
                .btn:hover { transform: translateY(-2px); box-shadow: 0 12px 24px rgba(102,126,234,.4); }
                .counter { margin-top: 28px; font-size: 40px; color: #667eea; font-weight: bold; }
                </style>
                </head>
                <body>
                <div class="card">
                  <h1>%s</h1>
                  <p>%s<br>这是一个由 AI Coding 平台生成的演示页面（Mock 模式）。</p>
                  <button class="btn" onclick="bump()">点我试试</button>
                  <div class="counter" id="counter">0</div>
                </div>
                <script>
                let n = 0;
                function bump() {
                  n += 1;
                  document.getElementById('counter').textContent = n;
                }
                </script>
                </body>
                </html>
                """.formatted(title, title, subtitle);
    }

    public static String indexHtml() {
        return demoHtml("AI Coding 演示应用", "输入你的需求，AI 将为你生成完整网页应用。");
    }
}
