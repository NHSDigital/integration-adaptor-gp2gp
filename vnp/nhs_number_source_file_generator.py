import sys, ast, random

file_name = sys.argv[1]
input_data = sys.argv[2]

if not file_name:
    raise Exception("Missing file name")

if not input_data:
    raise Exception("Missing input data")

input_data = ast.literal_eval(input_data)

if type(input_data) != dict:
    raise Exception("Input data is not a valid python dictionary")

nhs_numbers_list = []
for nhs_number in input_data.keys():
    count = input_data[nhs_number]
    if type(count) != int or count <= 0:
        raise Exception("Input data dictionary must have positive integer numbers as values")
    for i in range(count):
        nhs_numbers_list.append(nhs_number)

random.shuffle(nhs_numbers_list)

with open(file_name, "w") as f:
    for i in range(len(nhs_numbers_list)):
        nhs_number = nhs_numbers_list[i]
        f.write(nhs_number)
        if i < len(nhs_numbers_list):
            f.write("\n")

print("Successfully generate source file at '" + file_name + "'")