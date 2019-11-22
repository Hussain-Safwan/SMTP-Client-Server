// Client side C/C++ program to demonstrate Socket programming 
#include <stdio.h> 
#include <sys/socket.h> 
#include <stdlib.h> 
#include <netinet/in.h>
#include <unistd.h> 
#include <string.h> 
#include <unistd.h>
#include <arpa/inet.h>
#define PORT 8080 
#define MAXCHAR 1000

char username[100], filename[100], port[100], sub[100];

void strrev(char str[])
{
      char *p1, *p2;

      if (! str || ! *str)
            return;
      for (p1 = str, p2 = str + strlen(str) - 1; p2 > p1; ++p1, --p2)
      {
            *p1 ^= *p2;
            *p2 ^= *p1;
            *p1 ^= *p2;
      }
}

void seperate(char str[100]){
    int p=0, j=0;
    for (int i=0; str[i]; i++){
        if (str[i] == ':'){
            username[j] = '\0';
            p = j;
            break;
        }
        username[j] = str[i];
        j++;
    }
    j = 0;
    for(int i=p; str[i]; i++){
        port[j] = str[i];
        j++;
    }
    port[j] = '\0';
}

void receive(char str[100]){
    char user[100];
    for (int i=0; str[i]; i++){
        if (str[i] == ' '){
            int k=0;
            for (int j=i+1; str[j]!=' '; j++){
                user[k] = str[j];
                k++;
            }
            user[k] = '\0';
            break;
        }
    }

    seperate(user);

    int j=0;
    for (int i=strlen(str)-1; str[i]!=' '; i--){
        filename[j] = str[i];
        j++;
    }
    filename[j] = '\0';
    strrev(filename);

    for (int i=0; str[i]; i++){
        if (str[i] == '"'){
            int k=0;
            for (int j=i+1; str[j]!='"'; j++){
                sub[k] = str[j];
                k++;
            }
            sub[k] = '\0';
            break;
        }
    }
}


bool check(char str[100] ){
	if(str[0] == '2'){
		return true;
	}
	return false;
}


int main(int argc, char const *argv[]) 
{ 
    char para[1000];
	printf("Enter parameter: %s\n\n", para);
	scanf("%[^\n]s", para);
	receive(para);
	printf("Username: %s\nPort: %s\nSubject: %s\nFile: %s\n", username, port, sub, filename);
	struct sockaddr_in address; 
    int sock = 0, valread; 
    struct sockaddr_in serv_addr; 
    char hello[100] = "Hello from client"; 
    char buffer[1024] = {0}; 
    if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) 
    { 
        printf("\n Socket creation error \n"); 
        return -1; 
    } 
   
    memset(&serv_addr, '0', sizeof(serv_addr)); 
    
	serv_addr.sin_family = AF_INET; 
    serv_addr.sin_port = htons(PORT); 
       
    // Convert IPv4 and IPv6 addresses from text to binary form 
    if(inet_pton(AF_INET, "127.0.0.1", &serv_addr.sin_addr)<=0)  
    { 
        printf("\nInvalid address/ Address not supported \n"); 
        return -1; 
    } 
   
    if (connect(sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0) 
    { 
        printf("\nConnection Failed \n"); 
        return -1; 
    } 
	//   --- Connection Established ---

	//  -- Formalities ---


	read(sock, buffer, 1024);
	//printf("\n\nUser: %s\n\n", username);
	if (check(buffer)){
		printf("Server: %s\n", buffer);
		send(sock, "HELO", strlen("HELO"), 0);
		printf("Sent: HELO\n");
		read(sock, buffer, 1024);
		if (check(buffer)){
			printf("Server: %s\n", buffer);
			char mailFrom[100] = "MAIL FROM:<saf@gmail.com>";
			send(sock, mailFrom, strlen(mailFrom), 0);
			printf("Sent %s\n", mailFrom);

			read(sock, buffer, 1024);
			if (check(buffer)){
				printf("Server: %s\n", buffer);
				char recptTo[100] = "RCPT TO:<";
				strcat(recptTo, username);
				strcat(recptTo, ">");
				//printf("\n\nUser: %s\n\n", username);
				send(sock, recptTo, strlen(recptTo), 0);
				printf("Sent %s\n", recptTo);

				read(sock, buffer, 1024);
				if (check(buffer)){
					send(sock, sub, strlen(sub), 0); 
					printf("Sent subject: %s\n", sub);
					printf("Server: %s\nReady to transfer files...\n", buffer);
					//  --- File transfer ---
	
					printf("\nFile transfer...\n");
					FILE *fread;
				    char str[1000];
				    //char filename[100] = "in.txt";
				 
				    fread = fopen(filename, "r");
				    if (fread == NULL){
				        printf("Could not open file %s\n",filename);
				        return 1;
				    }
				    while (fgets(str, MAXCHAR, fread) != NULL){
				        printf(" -->  %s", str);
						send(sock, str, strlen(str), 0);
				    }
                    printf("\nFile trnasferred. Closing connection...\n");
				    fclose(fread);
				}
				else{
					printf("Server: %s\nFile not found. Closing connection...\n", buffer);
				}
			}
		}
	}	
	
	return 0; 
} 

