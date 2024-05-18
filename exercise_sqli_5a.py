import requests
import random

# Define WebGoat URL and credentials
webgoat_url = 'http://localhost:8080/WebGoat'
registration_url = f'{webgoat_url}/registration'
register_url = f'{webgoat_url}/register.mvc'
login_url = f'{webgoat_url}/j_spring_security_check'

sqlinjection5a_url = f'{webgoat_url}/SqlInjection/assignment5a'

# Define user credentials, crate username with random integer afer
username = 'ciguest' + str(random.randint(500, 1000))
password = '123guest'

sqlinjection_lesson_url = f'{webgoat_url}/start.mvc?username=' + username + '#lesson/SqlInjection.lesson/8'

# Start a session to keep the authentication state
session = requests.Session()

response = session.get(webgoat_url)
if response.status_code == 200:
    print('Accessed WebGoat home page.')

# Register a new user
response = session.get(registration_url)
if response.status_code == 200:
    print('Accessed registration page.')

# Send registration
print('Registering new user with username:', username)
register_payload = {
    'username': username,
    'password': password,
    'matchingPassword': password,
    'agree': 'agree'
}

response = session.post(register_url, data=register_payload)

if response.status_code != 200:
    print('Response headers:', response.headers)
    print(f'Failed to register user: {response.status_code}')
    exit()

# Login to WebGoat
login_payload = {
    'username': username,
    'password': password
}

response = session.post(login_url, data=login_payload)
if 'Invalid username or password' in response.text:
    print('Login failed. Check your credentials.')
    exit()

print('Login successful.')

# Access SQL Injection lesson page
response = session.get(sqlinjection_lesson_url)
if response.status_code == 200:
    print('Accessed SQL Injection lesson page.')
else:
    print(f'Failed to access SQL Injection lesson page: {response.status_code}')

# Perform SQL Injection exercise (lesson SQLInjection5a)
sqlinjection_payload = {
    'account': 'Smithcercise',
    'operator': 'or',
    'injection': "foo"
}

response = session.post(sqlinjection5a_url, data=sqlinjection_payload)
print("Exercised SQL injection code flow in 5a")
