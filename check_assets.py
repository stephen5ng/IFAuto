import os

assets_dir = '/Users/stephenng/programming/IFAuto/app/src/main/assets'
for filename in os.listdir(assets_dir):
    filepath = os.path.join(assets_dir, filename)
    if os.path.isfile(filepath):
        print(f"{filename}: {os.path.getsize(filepath)} bytes")
        with open(filepath, 'rb') as f:
            print(f"Content start: {f.read(100)}")
