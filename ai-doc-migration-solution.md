# AI生成技术文档迁移完整解决方案

## 🎯 方案概述

基于现有开源MCP工具的AI生成技术文档迁移解决方案，支持多种目标平台，提供自动化、批量化的文档迁移能力。

## 🛠️ 核心工具栈

### 1. 开源MCP服务器

#### MediaWiki MCP Server ⭐⭐⭐⭐⭐
```bash
# 安装
npm install -g @professional-wiki/mediawiki-mcp-server

# 配置
{
  "mcpServers": {
    "mediawiki": {
      "command": "npx",
      "args": ["@professional-wiki/mediawiki-mcp-server@latest"],
      "env": {
        "WIKI_SERVER": "your.wiki.domain",
        "OAUTH_TOKEN": "your_oauth_token"
      }
    }
  }
}
```

#### Confluence MCP Server ⭐⭐⭐⭐
```bash
# 克隆并构建
git clone https://github.com/sooperset/mcp-atlassian.git
cd mcp-atlassian
npm install && npm run build

# 配置
{
  "mcpServers": {
    "confluence": {
      "command": "node",
      "args": ["./dist/index.js"],
      "env": {
        "CONFLUENCE_API_TOKEN": "your_api_token",
        "CONFLUENCE_BASE_URL": "https://your-domain.atlassian.net",
        "CONFLUENCE_USER_EMAIL": "your_email"
      }
    }
  }
}
```

#### GitHub Wiki MCP Server ⭐⭐⭐⭐⭐
```bash
# 使用官方GitHub MCP Server
npm install -g @github/github-mcp-server

# 配置
{
  "mcpServers": {
    "github": {
      "command": "github-mcp-server",
      "env": {
        "GITHUB_TOKEN": "your_github_token"
      }
    }
  }
}
```

### 2. 格式转换工具

#### Pandoc Universal Converter
```bash
# 安装Pandoc
brew install pandoc  # macOS
sudo apt install pandoc  # Ubuntu

# 转换示例
pandoc input.md -t confluence -o output.confluence
pandoc input.md -t mediawiki -o output.wiki
pandoc input.md -t html -o output.html
```

#### 专用Python工具
```bash
# Confluence专用
pip install markdown-to-confluence

# MediaWiki专用  
pip install mwparserfromhell

# 通用Markdown处理
pip install markdown beautifulsoup4 html2text
```

## 📋 迁移流程设计

### 阶段1: 文档预处理
```python
# 文档解析和预处理脚本
import os
import re
import markdown
from pathlib import Path

class DocumentPreprocessor:
    def __init__(self, source_dir: str):
        self.source_dir = Path(source_dir)
        
    def scan_documents(self):
        """扫描所有文档文件"""
        return list(self.source_dir.rglob("*.md"))
    
    def extract_metadata(self, file_path: Path):
        """提取文档元数据"""
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # 提取YAML front matter
        if content.startswith('---'):
            parts = content.split('---', 2)
            if len(parts) >= 3:
                metadata = yaml.safe_load(parts[1])
                content = parts[2].strip()
            else:
                metadata = {}
        else:
            metadata = {}
            
        # 自动提取标题
        if not metadata.get('title'):
            title_match = re.search(r'^#\s+(.+)$', content, re.MULTILINE)
            if title_match:
                metadata['title'] = title_match.group(1)
                
        return metadata, content
    
    def validate_content(self, content: str):
        """验证内容格式"""
        issues = []
        
        # 检查图片链接
        img_pattern = r'!\[([^\]]*)\]\(([^)]+)\)'
        for match in re.finditer(img_pattern, content):
            img_path = match.group(2)
            if not img_path.startswith('http') and not Path(img_path).exists():
                issues.append(f"图片文件不存在: {img_path}")
                
        # 检查内部链接
        link_pattern = r'\[([^\]]+)\]\(([^)]+)\)'
        for match in re.finditer(link_pattern, content):
            link_url = match.group(2)
            if link_url.startswith('./') or link_url.endswith('.md'):
                issues.append(f"需要转换的内部链接: {link_url}")
                
        return issues
```

### 阶段2: 平台适配器
```python
# 平台特定的适配器
class PlatformAdapter:
    def __init__(self, platform_type: str, config: dict):
        self.platform_type = platform_type
        self.config = config
        
    def convert_content(self, content: str, metadata: dict):
        """转换内容格式"""
        if self.platform_type == "confluence":
            return self.convert_to_confluence(content, metadata)
        elif self.platform_type == "mediawiki":
            return self.convert_to_mediawiki(content, metadata)
        elif self.platform_type == "notion":
            return self.convert_to_notion(content, metadata)
        else:
            raise ValueError(f"不支持的平台: {self.platform_type}")
    
    def convert_to_confluence(self, content: str, metadata: dict):
        """转换为Confluence格式"""
        # 使用pandoc转换
        import subprocess
        
        process = subprocess.run([
            'pandoc', '-f', 'markdown', '-t', 'confluence'
        ], input=content, text=True, capture_output=True)
        
        if process.returncode != 0:
            raise Exception(f"Pandoc转换失败: {process.stderr}")
            
        confluence_content = process.stdout
        
        # 处理特殊元素
        confluence_content = self.process_confluence_elements(confluence_content)
        
        return {
            'title': metadata.get('title', 'Untitled'),
            'content': confluence_content,
            'space_key': self.config.get('space_key'),
            'parent_id': metadata.get('parent_id'),
            'labels': metadata.get('labels', [])
        }
    
    def convert_to_mediawiki(self, content: str, metadata: dict):
        """转换为MediaWiki格式"""
        # 使用pandoc转换
        import subprocess
        
        process = subprocess.run([
            'pandoc', '-f', 'markdown', '-t', 'mediawiki'
        ], input=content, text=True, capture_output=True)
        
        if process.returncode != 0:
            raise Exception(f"Pandoc转换失败: {process.stderr}")
            
        wiki_content = process.stdout
        
        return {
            'title': metadata.get('title', 'Untitled'),
            'content': wiki_content,
            'summary': metadata.get('summary', 'AI生成文档迁移'),
            'categories': metadata.get('categories', [])
        }
```

### 阶段3: MCP集成器
```python
# MCP客户端集成
import asyncio
import json
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

class MCPDocumentMigrator:
    def __init__(self, mcp_config: dict):
        self.mcp_config = mcp_config
        self.sessions = {}
        
    async def initialize_mcp_servers(self):
        """初始化MCP服务器连接"""
        for server_name, config in self.mcp_config.items():
            try:
                server_params = StdioServerParameters(
                    command=config['command'],
                    args=config.get('args', []),
                    env=config.get('env', {})
                )
                
                session = await stdio_client(server_params)
                await session.initialize()
                
                self.sessions[server_name] = session
                print(f"✅ MCP服务器 {server_name} 初始化成功")
                
            except Exception as e:
                print(f"❌ MCP服务器 {server_name} 初始化失败: {e}")
    
    async def migrate_to_confluence(self, document_data: dict):
        """迁移到Confluence"""
        if 'confluence' not in self.sessions:
            raise Exception("Confluence MCP服务器未初始化")
            
        session = self.sessions['confluence']
        
        try:
            # 调用MCP工具创建页面
            result = await session.call_tool(
                "create-page",
                {
                    "spaceKey": document_data['space_key'],
                    "title": document_data['title'],
                    "content": document_data['content'],
                    "parentId": document_data.get('parent_id')
                }
            )
            
            return {
                'success': True,
                'page_id': result.get('id'),
                'page_url': result.get('_links', {}).get('webui'),
                'platform': 'confluence'
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'platform': 'confluence'
            }
    
    async def migrate_to_mediawiki(self, document_data: dict):
        """迁移到MediaWiki"""
        if 'mediawiki' not in self.sessions:
            raise Exception("MediaWiki MCP服务器未初始化")
            
        session = self.sessions['mediawiki']
        
        try:
            # 调用MCP工具创建页面
            result = await session.call_tool(
                "create-page",
                {
                    "title": document_data['title'],
                    "content": document_data['content'],
                    "summary": document_data.get('summary', 'AI文档迁移')
                }
            )
            
            return {
                'success': True,
                'page_title': document_data['title'],
                'page_url': result.get('url'),
                'platform': 'mediawiki'
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'platform': 'mediawiki'
            }
    
    async def batch_migrate(self, documents: list, target_platform: str):
        """批量迁移文档"""
        results = []
        
        for i, doc in enumerate(documents):
            try:
                print(f"📄 迁移文档 {i+1}/{len(documents)}: {doc['title']}")
                
                if target_platform == 'confluence':
                    result = await self.migrate_to_confluence(doc)
                elif target_platform == 'mediawiki':
                    result = await self.migrate_to_mediawiki(doc)
                else:
                    result = {
                        'success': False,
                        'error': f'不支持的平台: {target_platform}'
                    }
                
                result['document_title'] = doc['title']
                results.append(result)
                
                # 添加延迟避免API限流
                await asyncio.sleep(1)
                
            except Exception as e:
                results.append({
                    'success': False,
                    'error': str(e),
                    'document_title': doc['title'],
                    'platform': target_platform
                })
        
        return results
```

## 🔄 使用示例

### 完整迁移脚本
```python
#!/usr/bin/env python3
"""
AI生成技术文档迁移脚本
支持批量迁移到Confluence、MediaWiki等平台
"""

import asyncio
import yaml
from pathlib import Path

async def main():
    # 1. 配置MCP服务器
    mcp_config = {
        "confluence": {
            "command": "node",
            "args": ["./confluence-mcp/dist/index.js"],
            "env": {
                "CONFLUENCE_API_TOKEN": "your_token",
                "CONFLUENCE_BASE_URL": "https://your-domain.atlassian.net",
                "CONFLUENCE_USER_EMAIL": "your_email"
            }
        },
        "mediawiki": {
            "command": "npx",
            "args": ["@professional-wiki/mediawiki-mcp-server@latest"],
            "env": {
                "WIKI_SERVER": "your.wiki.domain",
                "OAUTH_TOKEN": "your_oauth_token"
            }
        }
    }
    
    # 2. 初始化组件
    preprocessor = DocumentPreprocessor("./ai-generated-docs")
    adapter = PlatformAdapter("confluence", {"space_key": "DOCS"})
    migrator = MCPDocumentMigrator(mcp_config)
    
    # 3. 初始化MCP服务器
    await migrator.initialize_mcp_servers()
    
    # 4. 扫描和预处理文档
    doc_files = preprocessor.scan_documents()
    documents = []
    
    for file_path in doc_files:
        metadata, content = preprocessor.extract_metadata(file_path)
        issues = preprocessor.validate_content(content)
        
        if issues:
            print(f"⚠️  文档 {file_path} 存在问题:")
            for issue in issues:
                print(f"   - {issue}")
        
        # 转换格式
        converted_doc = adapter.convert_content(content, metadata)
        documents.append(converted_doc)
    
    # 5. 批量迁移
    print(f"🚀 开始迁移 {len(documents)} 个文档到 Confluence...")
    results = await migrator.batch_migrate(documents, "confluence")
    
    # 6. 生成报告
    successful = [r for r in results if r['success']]
    failed = [r for r in results if not r['success']]
    
    print(f"\n📊 迁移完成:")
    print(f"   ✅ 成功: {len(successful)}")
    print(f"   ❌ 失败: {len(failed)}")
    
    if failed:
        print(f"\n❌ 失败的文档:")
        for result in failed:
            print(f"   - {result['document_title']}: {result['error']}")

if __name__ == "__main__":
    asyncio.run(main())
```

## 📦 部署配置

### Docker Compose 部署
```yaml
version: '3.8'
services:
  doc-migrator:
    build: .
    environment:
      - CONFLUENCE_API_TOKEN=${CONFLUENCE_API_TOKEN}
      - CONFLUENCE_BASE_URL=${CONFLUENCE_BASE_URL}
      - CONFLUENCE_USER_EMAIL=${CONFLUENCE_USER_EMAIL}
      - MEDIAWIKI_OAUTH_TOKEN=${MEDIAWIKI_OAUTH_TOKEN}
      - MEDIAWIKI_SERVER=${MEDIAWIKI_SERVER}
    volumes:
      - ./docs:/app/docs
      - ./output:/app/output
    command: python migrate.py

  confluence-mcp:
    image: node:18
    working_dir: /app
    volumes:
      - ./confluence-mcp:/app
    command: npm start
    environment:
      - CONFLUENCE_API_TOKEN=${CONFLUENCE_API_TOKEN}
      - CONFLUENCE_BASE_URL=${CONFLUENCE_BASE_URL}

  mediawiki-mcp:
    image: node:18
    working_dir: /app
    command: npx @professional-wiki/mediawiki-mcp-server@latest
    environment:
      - WIKI_SERVER=${MEDIAWIKI_SERVER}
      - OAUTH_TOKEN=${MEDIAWIKI_OAUTH_TOKEN}
```

### GitHub Actions 自动化
```yaml
name: AI文档迁移
on:
  push:
    paths: ['docs/**/*.md']
  workflow_dispatch:

jobs:
  migrate-docs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'
          
      - name: Install dependencies
        run: |
          npm install -g @professional-wiki/mediawiki-mcp-server
          pip install -r requirements.txt
          
      - name: Run migration
        env:
          CONFLUENCE_API_TOKEN: ${{ secrets.CONFLUENCE_API_TOKEN }}
          CONFLUENCE_BASE_URL: ${{ secrets.CONFLUENCE_BASE_URL }}
          CONFLUENCE_USER_EMAIL: ${{ secrets.CONFLUENCE_USER_EMAIL }}
        run: python migrate.py
```

## 🎯 最佳实践

### 1. 迁移前准备
- ✅ 备份原始文档
- ✅ 验证目标平台权限
- ✅ 测试小批量迁移
- ✅ 准备回滚方案

### 2. 内容优化
- ✅ 统一图片存储路径
- ✅ 转换内部链接格式
- ✅ 添加适当的元数据
- ✅ 检查特殊字符兼容性

### 3. 监控和维护
- ✅ 设置迁移进度监控
- ✅ 记录详细的迁移日志
- ✅ 定期检查链接有效性
- ✅ 建立内容同步机制

这个方案基于现有的开源MCP工具，提供了完整的AI生成技术文档迁移解决方案，支持多种目标平台，具有良好的扩展性和维护性。
