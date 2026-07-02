import json
import os

base_dir = '/sessions/cool-sharp-cori/mnt/card-nav/src/data'
output_file = '/sessions/cool-sharp-cori/mnt/card-nav-java/card_nav/sql/init_data.sql'

def escape_sql(s):
    if s is None:
        return 'NULL'
    s = str(s).replace('\\', '\\\\').replace("'", "\\'").replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t')
    return f"'{s}'"

with open(output_file, 'w', encoding='utf-8') as out:
    out.write("-- card_nav 初始化数据\n\nUSE card_nav;\n\n")
    
    # data_record 文件
    for fname in ['sidebar', 'github', 'learning', 'portfolio', 'recruitment', 'tools', 'video-websites', 'ai-chat', 'favorites']:
        fpath = os.path.join(base_dir, f'{fname}.json')
        if os.path.exists(fpath):
            with open(fpath, 'r', encoding='utf-8') as f:
                data = json.load(f)
            content = json.dumps(data, ensure_ascii=False, indent=2)
            data_type = 'list' if isinstance(data, list) else 'map'
            out.write(f"INSERT INTO data_record (name, content, type, updated_at) VALUES ('{fname}', '{content.replace(chr(39), chr(92)+chr(39))}', '{data_type}', NOW()) ON DUPLICATE KEY UPDATE content=VALUES(content), type=VALUES(type), updated_at=NOW();\n")
    
    # clicks
    with open(os.path.join(base_dir, 'clicks.json'), 'r', encoding='utf-8') as f:
        clicks = json.load(f)
    out.write("\n-- clicks\n")
    for item in clicks:
        out.write(f"INSERT INTO clicks (card_id, card_title, sidebar_id, sidebar_label, category, favicon, count, created_at, updated_at) VALUES ({escape_sql(item.get('cardId'))}, {escape_sql(item.get('cardTitle'))}, {escape_sql(item.get('sidebarId'))}, {escape_sql(item.get('sidebarLabel'))}, {escape_sql(item.get('category'))}, {escape_sql(item.get('favicon'))}, {item.get('count', 0)}, STR_TO_DATE({escape_sql(item.get('createdAt'))}, '%Y-%m-%dT%H:%i:%s.%fZ'), STR_TO_DATE({escape_sql(item.get('updatedAt'))}, '%Y-%m-%dT%H:%i:%s.%fZ')) ON DUPLICATE KEY UPDATE count={item.get('count', 0)}, updated_at=STR_TO_DATE({escape_sql(item.get('updatedAt'))}, '%Y-%m-%dT%H:%i:%s.%fZ');\n")
    
    # visitors (前500)
    with open(os.path.join(base_dir, 'visitors.json'), 'r', encoding='utf-8') as f:
        visitors = json.load(f)[:500]
    out.write("\n-- visitors\n")
    for item in visitors:
        out.write(f"INSERT INTO visitors (ip, browser, device, timestamp) VALUES ({escape_sql(item.get('ip'))}, {escape_sql(item.get('browser'))}, {escape_sql(item.get('device'))}, STR_TO_DATE({escape_sql(item.get('timestamp'))}, '%Y-%m-%dT%H:%i:%s.%fZ'));\n")
    
    # logs
    with open(os.path.join(base_dir, 'logs.json'), 'r', encoding='utf-8') as f:
        logs = json.load(f)
    out.write("\n-- logs\n")
    for item in logs:
        content = json.dumps(item, ensure_ascii=False)
        out.write(f"INSERT INTO logs (content, created_at) VALUES ({escape_sql(content)}, NOW());\n")
    
    # stats
    with open(os.path.join(base_dir, 'stats.json'), 'r', encoding='utf-8') as f:
        stats = json.load(f)
    out.write("\n-- stats\n")
    out.write(f"UPDATE stats SET total_visits={stats.get('totalVisits', 0)}, last_7_days='{json.dumps(stats.get('last7Days', []), ensure_ascii=False)}', categories='{json.dumps(stats.get('categories', []), ensure_ascii=False)}', updated_at=NOW() WHERE id=1;\n")
    
    out.write("\nSELECT '数据初始化完成!' AS message;\n")

print("OK")
