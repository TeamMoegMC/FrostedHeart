import json

def add_cullface(data):
    # Check if 'elements' is in data
    if 'elements' in data:
        for element in data['elements']:
            # Check if 'faces' is in element
            if 'faces' in element:
                for face_name, face_data in element['faces'].items():
                    # Append cullface to each face
                    # In this example, we just set cullface to the face name
                    # You can customize this logic as needed.
                    face_data['cullface'] = face_name
    return data

def main(input_file, output_file):
    # Load JSON data from file
    with open(input_file, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # Process the data to add cullface
    data = add_cullface(data)

    # Write the updated JSON data to file
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)

if __name__ == "__main__":
    # Example usage:
    # Adjust 'input.json' and 'output.json' with your actual filenames.
    # main("input.json", "output.json")

    # get current dir
    import os
    print(os.getcwd())

    dir = "../src/main/resources/assets/frostedheart/models/block"
    # process all json files in the directory
    # for file in os.listdir(dir):
    #     if file.endswith(".json"):
    #         main(dir + "/" + file, dir + "/" + file)
    #         print("Processed: " + file)
    # scan all possible subdirectories
    for root, dirs, files in os.walk(dir):
        for file in files:
            if file.endswith(".json"):
                main(root + "/" + file, root + "/" + file)
                print("Processed: " + file)

