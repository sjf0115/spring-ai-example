---
name: skill-creator
description: Use this skill when the user asks to create, update, or scaffold a new Agent Skill. It guides writing a concise SKILL.md with valid YAML frontmatter and organizing reference files.
---

# Skill Creator

本技能用于帮助用户创建或更新一个 Agent Skill。

## 何时使用
- 用户要求"创建/新建一个技能(skill)"
- 用户要求为已有技能补充文档或参考资源

## 创建规范
1. 每个技能是一个独立目录,目录下必须包含 `SKILL.md`
2. `SKILL.md` 顶部必须是合法的 YAML frontmatter,至少包含:
   - `name`:技能名(建议与目录名一致,小写中划线)
   - `description`:以 "Use this skill when..." 开头,说明触发场景
3. 正文保持精简,把详细指引放到 `references/` 下的独立文件

## 输出要求
- 使用 write_text_file 在指定输出目录创建文件
- 完成后用一句话总结创建了哪些文件
