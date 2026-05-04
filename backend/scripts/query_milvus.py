from pymilvus import connections, Collection, utility
import sys

print("Starting script...", flush=True)
sys.stdout.flush()

try:
    print("Connecting to Milvus...", flush=True)
    connections.connect(host='localhost', port='19530')
    print("=== Connected to Milvus ===", flush=True)

    collections = utility.list_collections()
    print("Collections:", collections, flush=True)

    if 'violation_cases' in collections:
        collection = Collection('violation_cases')
        print("Entities count:", collection.num_entities, flush=True)
        collection.load()
        
        results = collection.query(
            expr='case_id >= 0',
            output_fields=['case_id', 'case_type', 'content', 'violation_reason', 'tags', 'chunk_index'],
            limit=100
        )
        
        print("=== Data List ===", flush=True)
        for i, r in enumerate(results, 1):
            print(f"[{i}] case_id: {r.get('case_id')}", flush=True)
            print(f"    case_type: {r.get('case_type')}", flush=True)
            content = r.get('content', '')
            if content:
                if len(content) > 80:
                    print(f"    content: {content[:80]}...", flush=True)
                else:
                    print(f"    content: {content}", flush=True)
            print(f"    violation_reason: {r.get('violation_reason')}", flush=True)
            print(f"    tags: {r.get('tags')}", flush=True)
            print(f"    chunk_index: {r.get('chunk_index')}", flush=True)
    else:
        print("violation_cases collection does not exist", flush=True)

    connections.disconnect('default')
    print("=== Query Complete ===", flush=True)
except Exception as e:
    print(f"Error: {e}", flush=True)
    import traceback
    traceback.print_exc()
