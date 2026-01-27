#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

// 获取目标目录（默认当前目录）
const targetDir = process.argv[2] || process.cwd();
const outputFilePath = path.join(targetDir, 'code.md');
const javaJsonFiles = [];

// 忽略的目录/文件（避免输出无关内容）
const ignoredItems = new Set(['node_modules', '.git', 'code.md', '.gradle', '.idea', 'build', 'out']);

/**
 * 构建树形目录结构
 * @param {string} dirPath - 当前目录路径
 * @param {string} prefix - 缩进前缀
 * @param {boolean} isLast - 是否为同级最后一个条目
 * @returns {string} 树形结构字符串
 */
function buildTree(dirPath, prefix = '', isLast = true) {
  const entries = fs.readdirSync(dirPath, { withFileTypes: true })
    .filter(entry => !ignoredItems.has(entry.name))
    .sort((a, b) => a.name.localeCompare(b.name));

  if (entries.length === 0) return '';

  let tree = '';
  const connector = isLast ? '└── ' : '├── ';
  const nextPrefix = prefix + (isLast ? '    ' : '│   ');

  // 添加当前目录名（根目录特殊处理）
  if (dirPath !== targetDir) {
    tree += prefix + connector + path.basename(dirPath) + '\n';
  }

  entries.forEach((entry, index) => {
    const isLastEntry = index === entries.length - 1;
    const entryPath = path.join(dirPath, entry.name);
    const linePrefix = (dirPath === targetDir ? '' : prefix) + (isLastEntry ? '└── ' : '├── ');

    if (entry.isDirectory()) {
      tree += linePrefix + entry.name + '/\n';
      tree += buildTree(entryPath, nextPrefix, isLastEntry);
    } else {
      tree += linePrefix + entry.name + '\n';
      
      // 收集 .java 和 .json 文件
      if (entry.name.endsWith('.java') || entry.name.endsWith('.json')) {
        javaJsonFiles.push(path.relative(targetDir, entryPath).replace(/\\/g, '/'));
      }
    }
  });

  return tree;
}

/**
 * 生成 code.md 文件
 */
function generateCodeMd() {
  try {
    // 1. 生成目录结构
    const treeStructure = buildTree(targetDir);
    let output = `# 目录结构\n\n${treeStructure.trim() || '（空目录）'}\n\n`;

    // 2. 按路径排序文件（保证输出一致性）
    javaJsonFiles.sort();

    // 3. 添加文件内容
    output += `# 文件内容\n\n`;
    javaJsonFiles.forEach((relativePath, index) => {
      const filePath = path.join(targetDir, relativePath);
      try {
        const content = fs.readFileSync(filePath, 'utf8');
        output += `\`\`\`${relativePath}\n${content}\n\`\`\`\n\n`;
      } catch (err) {
        console.error(`⚠️ 读取文件失败: ${relativePath}`, err.message);
      }
    });

    // 4. 写入文件
    fs.writeFileSync(outputFilePath, output.trim() + '\n', 'utf8');
    console.log(`✅ 成功生成: ${outputFilePath}`);
    console.log(`📊 共处理 ${javaJsonFiles.length} 个文件 (.java/.json)`);
  } catch (err) {
    console.error('❌ 生成失败:', err.message);
    process.exit(1);
  }
}

// 执行主流程
generateCodeMd();