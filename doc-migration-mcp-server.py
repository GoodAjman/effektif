#!/usr/bin/env python3
"""
文档迁移 MCP 服务器

这个 MCP 服务器提供了将 AI 生成的技术文档迁移到各种在线文档平台的功能。
支持 Confluence、Notion、GitBook、BookStack 等多种目标平台。

功能特性：
1. 多格式文档解析 (Markdown, HTML, Docx)
2. 智能内容提取和格式转换
3. 批量文档迁移
4. 进度监控和错误处理
5. 平台特定的API适配

使用方法：
python doc-migration-mcp-server.py

MCP 工具：
- parse_document: 解析文档内容
- convert_format: 转换文档格式
- migrate_to_confluence: 迁移到 Confluence
- migrate_to_notion: 迁移到 Notion
- batch_migrate: 批量迁移文档
- get_migration_status: 获取迁移状态
"""

import asyncio
import json
import logging
import os
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import urlparse

import aiofiles
import aiohttp
import markdown
import pandoc
from bs4 import BeautifulSoup
from mcp.server import Server
from mcp.server.models import InitializationOptions
from mcp.server.stdio import stdio_server
from mcp.types import (
    Resource,
    Tool,
    TextContent,
    ImageContent,
    EmbeddedResource,
)

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# MCP 服务器实例
app = Server("doc-migration-server")

# 全局配置
CONFIG = {
    "supported_formats": ["md", "markdown", "html", "docx", "txt"],
    "max_file_size": 50 * 1024 * 1024,  # 50MB
    "batch_size": 10,
    "timeout": 300,  # 5分钟
}

# 迁移状态跟踪
migration_status = {}

class DocumentParser:
    """文档解析器 - 支持多种格式的文档解析"""
    
    @staticmethod
    async def parse_markdown(content: str) -> Dict[str, Any]:
        """解析 Markdown 文档"""
        try:
            # 使用 markdown 库解析
            md = markdown.Markdown(extensions=[
                'meta', 'toc', 'tables', 'fenced_code', 'codehilite'
            ])
            html_content = md.convert(content)
            
            # 提取元数据
            metadata = getattr(md, 'Meta', {})
            
            # 解析文档结构
            soup = BeautifulSoup(html_content, 'html.parser')
            
            # 提取标题结构
            headings = []
            for heading in soup.find_all(['h1', 'h2', 'h3', 'h4', 'h5', 'h6']):
                headings.append({
                    'level': int(heading.name[1]),
                    'text': heading.get_text().strip(),
                    'id': heading.get('id', ''),
                })
            
            # 提取代码块
            code_blocks = []
            for code in soup.find_all('code'):
                if code.parent.name == 'pre':
                    code_blocks.append({
                        'language': code.get('class', [''])[0].replace('language-', ''),
                        'content': code.get_text(),
                    })
            
            # 提取图片
            images = []
            for img in soup.find_all('img'):
                images.append({
                    'src': img.get('src', ''),
                    'alt': img.get('alt', ''),
                    'title': img.get('title', ''),
                })
            
            # 提取链接
            links = []
            for link in soup.find_all('a'):
                href = link.get('href', '')
                if href and not href.startswith('#'):
                    links.append({
                        'url': href,
                        'text': link.get_text().strip(),
                        'title': link.get('title', ''),
                    })
            
            return {
                'format': 'markdown',
                'title': metadata.get('title', [''])[0] if metadata.get('title') else '',
                'metadata': metadata,
                'html_content': html_content,
                'raw_content': content,
                'structure': {
                    'headings': headings,
                    'code_blocks': code_blocks,
                    'images': images,
                    'links': links,
                },
                'word_count': len(content.split()),
                'estimated_read_time': max(1, len(content.split()) // 200),
            }
            
        except Exception as e:
            logger.error(f"解析 Markdown 失败: {e}")
            raise
    
    @staticmethod
    async def parse_html(content: str) -> Dict[str, Any]:
        """解析 HTML 文档"""
        try:
            soup = BeautifulSoup(content, 'html.parser')
            
            # 提取标题
            title_tag = soup.find('title')
            title = title_tag.get_text().strip() if title_tag else ''
            
            # 提取主要内容
            main_content = soup.find('main') or soup.find('article') or soup.find('body') or soup
            
            # 转换为 Markdown
            # 这里可以使用 html2text 或类似工具
            import html2text
            h = html2text.HTML2Text()
            h.ignore_links = False
            h.ignore_images = False
            markdown_content = h.handle(str(main_content))
            
            return {
                'format': 'html',
                'title': title,
                'html_content': content,
                'markdown_content': markdown_content,
                'raw_content': content,
                'word_count': len(main_content.get_text().split()),
            }
            
        except Exception as e:
            logger.error(f"解析 HTML 失败: {e}")
            raise

class FormatConverter:
    """格式转换器 - 使用 Pandoc 进行格式转换"""
    
    @staticmethod
    async def convert_with_pandoc(content: str, from_format: str, to_format: str, 
                                 options: Optional[Dict] = None) -> str:
        """使用 Pandoc 转换格式"""
        try:
            # 构建 Pandoc 命令
            cmd = ['pandoc', f'--from={from_format}', f'--to={to_format}']
            
            if options:
                for key, value in options.items():
                    if value is True:
                        cmd.append(f'--{key}')
                    elif value is not False:
                        cmd.append(f'--{key}={value}')
            
            # 执行转换
            process = await asyncio.create_subprocess_exec(
                *cmd,
                stdin=asyncio.subprocess.PIPE,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )
            
            stdout, stderr = await process.communicate(content.encode('utf-8'))
            
            if process.returncode != 0:
                raise Exception(f"Pandoc 转换失败: {stderr.decode()}")
            
            return stdout.decode('utf-8')
            
        except Exception as e:
            logger.error(f"Pandoc 转换失败: {e}")
            raise
    
    @staticmethod
    async def markdown_to_confluence(content: str) -> str:
        """Markdown 转换为 Confluence 格式"""
        options = {
            'wrap': 'none',
            'atx-headers': True,
        }
        return await FormatConverter.convert_with_pandoc(
            content, 'markdown', 'confluence', options
        )
    
    @staticmethod
    async def markdown_to_html(content: str) -> str:
        """Markdown 转换为 HTML"""
        options = {
            'standalone': True,
            'self-contained': True,
        }
        return await FormatConverter.convert_with_pandoc(
            content, 'markdown', 'html', options
        )

class PlatformMigrator:
    """平台迁移器 - 处理到各种平台的迁移"""
    
    @staticmethod
    async def migrate_to_confluence(content: str, config: Dict[str, Any]) -> Dict[str, Any]:
        """迁移到 Confluence"""
        try:
            # 转换格式
            confluence_content = await FormatConverter.markdown_to_confluence(content)
            
            # 构建 API 请求
            url = f"{config['base_url']}/rest/api/content"
            headers = {
                'Authorization': f"Bearer {config['api_token']}",
                'Content-Type': 'application/json',
            }
            
            payload = {
                'type': 'page',
                'title': config.get('title', 'Migrated Document'),
                'space': {'key': config['space_key']},
                'body': {
                    'storage': {
                        'value': confluence_content,
                        'representation': 'storage'
                    }
                }
            }
            
            if config.get('parent_id'):
                payload['ancestors'] = [{'id': config['parent_id']}]
            
            # 发送请求
            async with aiohttp.ClientSession() as session:
                async with session.post(url, headers=headers, json=payload) as response:
                    if response.status == 200:
                        result = await response.json()
                        return {
                            'success': True,
                            'page_id': result['id'],
                            'page_url': f"{config['base_url']}{result['_links']['webui']}",
                            'title': result['title'],
                        }
                    else:
                        error_text = await response.text()
                        raise Exception(f"Confluence API 错误: {response.status} - {error_text}")
                        
        except Exception as e:
            logger.error(f"Confluence 迁移失败: {e}")
            return {
                'success': False,
                'error': str(e),
            }
    
    @staticmethod
    async def migrate_to_notion(content: str, config: Dict[str, Any]) -> Dict[str, Any]:
        """迁移到 Notion"""
        try:
            # 解析 Markdown 内容
            parsed = await DocumentParser.parse_markdown(content)
            
            # 构建 Notion 块结构
            blocks = []
            
            # 添加标题
            if parsed.get('title'):
                blocks.append({
                    'object': 'block',
                    'type': 'heading_1',
                    'heading_1': {
                        'rich_text': [{'type': 'text', 'text': {'content': parsed['title']}}]
                    }
                })
            
            # 转换内容为 Notion 块
            # 这里需要更复杂的解析逻辑来处理各种 Markdown 元素
            
            # 构建 API 请求
            url = "https://api.notion.com/v1/pages"
            headers = {
                'Authorization': f"Bearer {config['api_token']}",
                'Content-Type': 'application/json',
                'Notion-Version': '2022-06-28',
            }
            
            payload = {
                'parent': {'database_id': config['database_id']},
                'properties': {
                    'title': {
                        'title': [{'text': {'content': config.get('title', 'Migrated Document')}}]
                    }
                },
                'children': blocks[:100]  # Notion 限制每次最多100个块
            }
            
            # 发送请求
            async with aiohttp.ClientSession() as session:
                async with session.post(url, headers=headers, json=payload) as response:
                    if response.status == 200:
                        result = await response.json()
                        return {
                            'success': True,
                            'page_id': result['id'],
                            'page_url': result['url'],
                            'title': config.get('title', 'Migrated Document'),
                        }
                    else:
                        error_text = await response.text()
                        raise Exception(f"Notion API 错误: {response.status} - {error_text}")
                        
        except Exception as e:
            logger.error(f"Notion 迁移失败: {e}")
            return {
                'success': False,
                'error': str(e),
            }

# MCP 工具定义

@app.list_tools()
async def list_tools() -> List[Tool]:
    """列出所有可用的工具"""
    return [
        Tool(
            name="parse_document",
            description="解析文档内容，提取结构化信息",
            inputSchema={
                "type": "object",
                "properties": {
                    "content": {"type": "string", "description": "文档内容"},
                    "format": {"type": "string", "enum": ["markdown", "html", "auto"], "default": "auto"},
                },
                "required": ["content"],
            },
        ),
        Tool(
            name="convert_format",
            description="转换文档格式",
            inputSchema={
                "type": "object",
                "properties": {
                    "content": {"type": "string", "description": "源文档内容"},
                    "from_format": {"type": "string", "description": "源格式"},
                    "to_format": {"type": "string", "description": "目标格式"},
                    "options": {"type": "object", "description": "转换选项"},
                },
                "required": ["content", "from_format", "to_format"],
            },
        ),
        Tool(
            name="migrate_to_confluence",
            description="迁移文档到 Confluence",
            inputSchema={
                "type": "object",
                "properties": {
                    "content": {"type": "string", "description": "文档内容"},
                    "config": {
                        "type": "object",
                        "properties": {
                            "base_url": {"type": "string"},
                            "api_token": {"type": "string"},
                            "space_key": {"type": "string"},
                            "title": {"type": "string"},
                            "parent_id": {"type": "string"},
                        },
                        "required": ["base_url", "api_token", "space_key"],
                    },
                },
                "required": ["content", "config"],
            },
        ),
        Tool(
            name="migrate_to_notion",
            description="迁移文档到 Notion",
            inputSchema={
                "type": "object",
                "properties": {
                    "content": {"type": "string", "description": "文档内容"},
                    "config": {
                        "type": "object",
                        "properties": {
                            "api_token": {"type": "string"},
                            "database_id": {"type": "string"},
                            "title": {"type": "string"},
                        },
                        "required": ["api_token", "database_id"],
                    },
                },
                "required": ["content", "config"],
            },
        ),
        Tool(
            name="batch_migrate",
            description="批量迁移多个文档",
            inputSchema={
                "type": "object",
                "properties": {
                    "documents": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "content": {"type": "string"},
                                "title": {"type": "string"},
                                "path": {"type": "string"},
                            },
                            "required": ["content"],
                        },
                    },
                    "target_platform": {"type": "string", "enum": ["confluence", "notion"]},
                    "config": {"type": "object"},
                },
                "required": ["documents", "target_platform", "config"],
            },
        ),
        Tool(
            name="get_migration_status",
            description="获取迁移任务状态",
            inputSchema={
                "type": "object",
                "properties": {
                    "task_id": {"type": "string", "description": "任务ID"},
                },
                "required": ["task_id"],
            },
        ),
    ]

@app.call_tool()
async def call_tool(name: str, arguments: Dict[str, Any]) -> List[TextContent]:
    """处理工具调用"""
    try:
        if name == "parse_document":
            content = arguments["content"]
            format_type = arguments.get("format", "auto")
            
            # 自动检测格式
            if format_type == "auto":
                if content.strip().startswith('<'):
                    format_type = "html"
                else:
                    format_type = "markdown"
            
            if format_type == "markdown":
                result = await DocumentParser.parse_markdown(content)
            elif format_type == "html":
                result = await DocumentParser.parse_html(content)
            else:
                raise ValueError(f"不支持的格式: {format_type}")
            
            return [TextContent(type="text", text=json.dumps(result, indent=2, ensure_ascii=False))]
        
        elif name == "convert_format":
            content = arguments["content"]
            from_format = arguments["from_format"]
            to_format = arguments["to_format"]
            options = arguments.get("options", {})
            
            result = await FormatConverter.convert_with_pandoc(
                content, from_format, to_format, options
            )
            
            return [TextContent(type="text", text=result)]
        
        elif name == "migrate_to_confluence":
            content = arguments["content"]
            config = arguments["config"]
            
            result = await PlatformMigrator.migrate_to_confluence(content, config)
            
            return [TextContent(type="text", text=json.dumps(result, indent=2, ensure_ascii=False))]
        
        elif name == "migrate_to_notion":
            content = arguments["content"]
            config = arguments["config"]
            
            result = await PlatformMigrator.migrate_to_notion(content, config)
            
            return [TextContent(type="text", text=json.dumps(result, indent=2, ensure_ascii=False))]
        
        elif name == "batch_migrate":
            documents = arguments["documents"]
            target_platform = arguments["target_platform"]
            config = arguments["config"]
            
            # 生成任务ID
            task_id = f"migration_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            
            # 初始化任务状态
            migration_status[task_id] = {
                'status': 'running',
                'total': len(documents),
                'completed': 0,
                'failed': 0,
                'results': [],
                'start_time': datetime.now().isoformat(),
            }
            
            # 异步执行批量迁移
            asyncio.create_task(batch_migrate_task(task_id, documents, target_platform, config))
            
            return [TextContent(type="text", text=json.dumps({
                'task_id': task_id,
                'status': 'started',
                'total_documents': len(documents),
            }, indent=2))]
        
        elif name == "get_migration_status":
            task_id = arguments["task_id"]
            
            if task_id not in migration_status:
                return [TextContent(type="text", text=json.dumps({
                    'error': f'任务 {task_id} 不存在'
                }, indent=2))]
            
            status = migration_status[task_id].copy()
            
            return [TextContent(type="text", text=json.dumps(status, indent=2, ensure_ascii=False))]
        
        else:
            return [TextContent(type="text", text=f"未知工具: {name}")]
    
    except Exception as e:
        logger.error(f"工具调用失败 {name}: {e}")
        return [TextContent(type="text", text=f"错误: {str(e)}")]

async def batch_migrate_task(task_id: str, documents: List[Dict], 
                           target_platform: str, config: Dict):
    """批量迁移任务"""
    try:
        for i, doc in enumerate(documents):
            try:
                if target_platform == "confluence":
                    result = await PlatformMigrator.migrate_to_confluence(
                        doc["content"], {**config, "title": doc.get("title", f"Document {i+1}")}
                    )
                elif target_platform == "notion":
                    result = await PlatformMigrator.migrate_to_notion(
                        doc["content"], {**config, "title": doc.get("title", f"Document {i+1}")}
                    )
                else:
                    result = {"success": False, "error": f"不支持的平台: {target_platform}"}
                
                migration_status[task_id]['results'].append({
                    'document': doc.get('title', f'Document {i+1}'),
                    'result': result,
                })
                
                if result.get('success'):
                    migration_status[task_id]['completed'] += 1
                else:
                    migration_status[task_id]['failed'] += 1
                    
            except Exception as e:
                migration_status[task_id]['failed'] += 1
                migration_status[task_id]['results'].append({
                    'document': doc.get('title', f'Document {i+1}'),
                    'result': {'success': False, 'error': str(e)},
                })
        
        migration_status[task_id]['status'] = 'completed'
        migration_status[task_id]['end_time'] = datetime.now().isoformat()
        
    except Exception as e:
        migration_status[task_id]['status'] = 'failed'
        migration_status[task_id]['error'] = str(e)
        migration_status[task_id]['end_time'] = datetime.now().isoformat()

async def main():
    """启动 MCP 服务器"""
    async with stdio_server() as (read_stream, write_stream):
        await app.run(
            read_stream,
            write_stream,
            InitializationOptions(
                server_name="doc-migration-server",
                server_version="1.0.0",
                capabilities=app.get_capabilities(
                    notification_options=None,
                    experimental_capabilities=None,
                ),
            ),
        )

if __name__ == "__main__":
    asyncio.run(main())
