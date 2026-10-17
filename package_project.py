import os
import zipfile

def create_zip(output_zip_path):
    exclude_dirs = {'.gradle', 'build', '.git', '.idea', 'captures', '.externalNativeBuild'}
    exclude_files = {'.DS_Store', 'local.properties', output_zip_path}

    print(f"Creating ZIP archive: {output_zip_path}...")
    file_count = 0
    with zipfile.ZipFile(output_zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk('.'):
            # تصفية المجلدات المستبعدة
            dirs[:] = [d for d in dirs if d not in exclude_dirs and not d.startswith('.gradle')]
            for file in files:
                if file in exclude_files or file.endswith('.zip') or file.endswith('.hprof'):
                    continue
                file_path = os.path.join(root, file)
                # حساب المسار النسبي داخل ملف الـ zip
                rel_path = os.path.relpath(file_path, '.')
                if rel_path.startswith('./'):
                    rel_path = rel_path[2:]
                zipf.write(file_path, rel_path)
                file_count += 1

    file_size_mb = os.path.getsize(output_zip_path) / (1024 * 1024)
    print(f"SUCCESS: Created {output_zip_path} with {file_count} files ({file_size_mb:.2f} MB).")

if __name__ == '__main__':
    create_zip("DeepFinder_Android_Project.zip")
