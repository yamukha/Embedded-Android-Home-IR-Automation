/*----------------------------------------------------------------------------
 Copyright:      Radig Ulrich  mailto: mail@ulrichradig.de
 Author:         Radig Ulrich
 Remarks:        
 known Problems: none
 Version:        24.10.2007
 Description:    Webserver uvm.

 Dieses Programm ist freie Software. Sie können es unter den Bedingungen der 
 GNU General Public License, wie von der Free Software Foundation veröffentlicht, 
 weitergeben und/oder modifizieren, entweder gemäß Version 2 der Lizenz oder 
 (nach Ihrer Option) jeder späteren Version. 

 Die Veröffentlichung dieses Programms erfolgt in der Hoffnung, 
 daß es Ihnen von Nutzen sein wird, aber OHNE IRGENDEINE GARANTIE, 
 sogar ohne die implizite Garantie der MARKTREIFE oder der VERWENDBARKEIT 
 FÜR EINEN BESTIMMTEN ZWECK. Details finden Sie in der GNU General Public License. 

 Sie sollten eine Kopie der GNU General Public License zusammen mit diesem 
 Programm erhalten haben. 
 Falls nicht, schreiben Sie an die Free Software Foundation, 
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA. 
----------------------------------------------------------------------------*/

#include <avr/io.h>
#include "config.h"
#include "usart.h"
#include "networkcard/enc28j60.h"
#include "networkcard/rtl8019.h"
#include "stack.h"
#include "timer.h"
#include "wol.h"
#include "httpd.h"
#include "cmd.h"
#include "telnetd.h"
#include "ntp.h"
#include "base64.h"
#include "http_get.h"
#include "lcd.h"
#include "udp_lcd.h"
#include "analog.h"
#include "camera/cam.h"
#include "camera/servo.h"
#include "sendmail.h"
#include <avr/eeprom.h>

#include "dhcpc.h"
#include "dnsc.h"
#include <util/delay.h>

#define IR_FREQ 36000

#define ADC_IN7 7
#define ADC_VREF_TYPE 0x40



// Read the AD conversion result
unsigned int read_adc(unsigned char adc_input)
{
    ADCSRA=0x82;
    ADMUX=adc_input | (ADC_VREF_TYPE & 0xff);
    // Delay needed for the stabilization of the ADC input voltage
    _delay_us(10);
    // Start the AD conversion
    ADCSRA|=0x40;
    // Wait for the AD conversion to complete
    while ((ADCSRA & 0x10)==0);
	ADCSRA &= ~0x10;
    return ( (0x3 & ADCH) * 256 + ADCL );
}

#define set_e()  (PORTD |= 0b00010000)
#define set_rs() (PORTD |= 0b00100000)   
#define set_rw() (PORTD |= 0b01000000)   
  
#define clr_e()  (PORTD &= 0b11101111)   
#define clr_rs() (PORTD &= 0b11011111)   
#define clr_rw() (PORTD &= 0b10111111)   

//ôóíêöèÿ çàïèñè êîìàíäû 
void LcdWriteCom(unsigned char data)
{ 
  clr_rs();
  _delay_us(2); 
  clr_rw(); 
  _delay_us(2);
     
  PORTC = data; 
  _delay_us(2);
  set_e();  
   _delay_us(2);
  clr_e();
   _delay_us(2);
  set_rw(); 
  _delay_us(2);
   set_rs();  
  _delay_us(10); 
 
}


void LcdWriteData(unsigned char data)
{
  set_rs ();
  _delay_us(2); 
  clr_rw();   
  _delay_us(2); 
  
  PORTC = data;		//
  _delay_us(2);
  set_e(); 
  _delay_us(2);
  clr_e ();
  _delay_us(2);
  set_rw(); 
  _delay_us(10);
  
}

void InitLcd(void)
{ 
  _delay_ms(40);
  LcdWriteCom(0x38); //0b00111000 - 8 bus 8 bits, 2 lines
  _delay_us(40);
  LcdWriteCom(0xf);  //0b00001111 - LCD, cursor, blink on
  _delay_us(40);
  LcdWriteCom(0x1);  //0b00000001 - clr LCD
  _delay_ms(40);
  LcdWriteCom(0x6);  //0b00000110 - cursor move right, no shift
  _delay_us(40);     
}

int LCD_test( void )
{
  LcdWriteData('T');
  LcdWriteData('e');
  LcdWriteData('s');
  LcdWriteData('t');
  LcdWriteData('.');
  return 0;
}

unsigned int IRC_code_array [67] = 
	{
	 9026,   4442,    616,    501,   605,    1654,
     604,    1615,    594,    1647,  592,     528,
     615,    1618,    622,    1621,  619,     502,
     589,    1650,    615,    505,   624,     498,
     626,    497,     621,    1622,  604,     491,
     617,    503,     616,    1623,  615,     502,
     620,    501,     616,    505,   615,    1619,
     621,    500,     596,    526,   636,    1618,
     603,    500,     596,    1643,  595,    1646,
     593,    1646,    593,    528,   615,    1619,
     620,    1621,    629,    493,   593,    1646,
    615
	};

int IRC_options (unsigned int * IRC_code, unsigned int data_len)
{
// Timer2 setup for carrier 36KHz
	PORTD |= 0x80;
    TCCR2 |= (1<<WGM21) + (1<<COM20)+ (1<<CS20);
	OCR2= (F_CPU / (IR_FREQ * 4L) - 1);
    
	// IR control
    {   int a;
		PORTD |= 0x80;
	
	    // at the and IR LED  off
		TCCR2 &= ~(1<<COM20);
		TCCR2 |= (1<<COM21);
		TCCR2 |= (1<<CS20);
		_delay_ms (1);
				
				
		// toggle LED 
		TCCR2 &= ~(1<<COM21);
		TCCR2 |= (1<<COM20);
				
		cli ();
		
		for (a=0;a<data_len;a++)
		
		{
			if (IRC_code[a] > 800)
				_delay_us ( IRC_code[a] / 2);
				
			else 
				_delay_us ( (IRC_code[a] / 20)  * 8);
						
			TCCR2 ^= (1<<CS20);	// toggle  carrirer
			 
		}
		
		TCCR2 &= ~(1<<CS20);
		PORTD &= ~0x80;
		sei(); 
		
	}
	return 0;
}
//----------------------------------------------------------------------------
//Hier startet das Hauptprogramm
int main(void)
{  
    
	//Konfiguration der Ausgänge bzw. Eingänge
	//definition erfolgt in der config.h
	DDRA = OUTA;
	//DDRC = OUTC;
	//DDRD = OUTD;	
	DDRC = OUTC | 0xFF;
	DDRD = OUTD | 0xF0;
	
	PORTA |= 0b00000001;
	PORTD |= 0x10;
	
	InitLcd();
    LCD_test ();	
			
    unsigned long a;
//	extern unsigned char myLAN[6];

	#if USE_SERVO
		servo_init ();
	#endif //USE_SERVO
	
    usart_init(BAUDRATE); // setup the UART
	
	#if USE_ADC
		ADC_Init();
		usart_write("\n\r ADC initalized \r\n");
	#endif
    unsigned int ADC_data;
	ADC_data = read_adc(ADC_IN7) - 17;	
	
    usart_write("System Ready \r\n");
  	usart_write("Compiled "__DATE__" at "__TIME__"\r\n");
    usart_write("Done with WinAVR & GCC Version "__VERSION__"\r\n");
	usart_write("USART with UBRR => %1i\r\n", UBRR);
	usart_write("Read Analog input %1i, ADC data => %1i \r\n", ADC_IN7, ADC_data/2);
   
  
	for(a=0;a<1000000;a++){asm("nop");};

	//Applikationen starten
	stack_init();
	httpd_init();
	telnetd_init();
	
	//Spielerrei mit einem LCD
	#if USE_SER_LCD
	udp_lcd_init();
	lcd_init();
	lcd_clear();
	back_light = 1;
	lcd_print(0,0,"System Ready");
	#endif
	//Ethernetcard Interrupt enable
	ETH_INT_ENABLE;
	
	//Globale Interrupts einschalten
	sei(); 
	
	#if USE_CAM
		#if USE_SER_LCD
		lcd_print(1,0,"CAMERA INIT");
		#endif //USE_SER_LCD
	for(a=0;a<2000000;a++){asm("nop");};
	cam_init();
	max_bytes = cam_picture_store(CAM_RESOLUTION);
		#if USE_SER_LCD
		back_light = 0;
		lcd_print(1,0,"CAMERA READY");
		#endif //USE_SER_LCD
	#endif //USE_CAM

    #if USE_DHCP
	usart_write("Start DHCP client \r\n");

    dhcp_init();
    if ( dhcp() == 0)
    {
        save_ip_addresses();
    }
    else
    {
        usart_write("DHCP fail\r\n");
        read_ip_addresses(); //get from EEPROM
    }
	
    #endif //USE_DHCP
	
    usart_write("\r\nIP   %1i.%1i.%1i.%1i\r\n", myip[0]     , myip[1]     , myip[2]     , myip[3]);
    usart_write("MASK %1i.%1i.%1i.%1i\r\n", netmask[0]  , netmask[1]  , netmask[2]  , netmask[3]);
    usart_write("GW   %1i.%1i.%1i.%1i\r\n", router_ip[0], router_ip[1], router_ip[2], router_ip[3]);

    #if USE_DNS
   // usart_write("DNS  %1i.%1i.%1i.%1i\r\n", dns_server_ip[0], dns_server_ip[1], dns_server_ip[2], dns_server_ip[3]);
    #endif //USE_DNS
    
	
	
	#if USE_UDP_DISCOVERY
	
	//_delay_ms(100);
	
	udp_discovery_packet (myip,mymac,bcast_ip,bcast_MAC,2604,14666,UDP_discover_pkt,UDP_DISCOVER_PKT_LEN);
	usart_write("Sent discover packet \r\n");
	//_delay_ms(100);
    command_arp ();
	
	#endif
	
	#if USE_NTP
        #if USE_DNS
        dns_init();
        if ( dns_resolve("1.de.pool.ntp.org") == 0) //resolve NTP server
        {
          for (unsigned char count = 0; count<4; count++)
          {
            eeprom_busy_wait ();
            eeprom_write_byte((unsigned char *)(NTP_IP_EEPROM_STORE + count),dns_resolved_ip[count]);
          }
		   usart_write("DNS  %1i.%1i.%1i.%1i\r\n", dns_server_ip[0], dns_server_ip[1], dns_server_ip[2], dns_server_ip[3]);
        }
        else
        {
            usart_write("DNS Err.\r\n");
        }
        #endif //USE_DNS
    
   ntp_init(); 
   usart_write("NTP started \r\n");
   
   for(a=0;a<1000000;a++){asm("nop");};
   
   ntp_request();
   
    if ( ntp() != 0 )
	{
	  usart_write("NTP Err.\r\n");
	}
    else
	{
	  command_time();
	}
	#endif //USE_NTP
	
	#if USE_WOL
        wol_init();
	#endif //USE_WOL
    
    #if USE_MAIL
        mail_client_init();
	#endif //USE_MAIL  
		
   
	
	while(1)
	{
		#if USE_ADC
		ANALOG_ON;
		#endif
	    eth_get_data();
		if (eth_buffer_size >  0)
		{
		   // if ( (0 == [13]) )
		        unsigned int payload_len ;  
				unsigned int options_len ;  
		        payload_len = 256 * eth_buffer [UDP_DATA_LEN] + eth_buffer [UDP_DATA_LEN + 1];
				options_len = (payload_len - 8 -6 )/2;
			    usart_write("Payload size is %i \r\n",eth_buffer[12], eth_buffer[13]);
			    eth_MAC_dst [0] = eth_buffer[6]; 	eth_MAC_dst [1] = eth_buffer[7];
                eth_MAC_dst [2] = eth_buffer[8];   eth_MAC_dst [3] = eth_buffer[9];
          	    eth_MAC_dst [4] = eth_buffer[10];  eth_MAC_dst [5] = eth_buffer[11];
			    eth_IP_dst  [0] = eth_buffer[26]; 	eth_IP_dst  [1] = eth_buffer[27];; 
			    eth_IP_dst  [2] = eth_buffer[28];  eth_IP_dst  [3] = eth_buffer[29]; ;
			
		     	unsigned int dst_port;
			    unsigned int src_port;
			
		    	dst_port = eth_buffer[34] * 256 + eth_buffer[35];
			    src_port = eth_buffer[36] * 256 + eth_buffer[37];
			
			    unsigned char command_list   [] = "IR|ON|OFF|ADC|";
				unsigned char command_LED_On [] = "LED On";
				unsigned char command_LED_Off[] = "LED Off";
//				unsigned char command_ADC_Rd [] = "Read ADC";
				unsigned char command_IRC    [] = "IR control";
				unsigned char data_string[16];
				
						
			if ( (0x08 == eth_buffer [12]) && (0x00 == eth_buffer [13]) )
			{
		    
			ENC_DEBUG (" Received packet \r\n");
                    for ( a =0; a< eth_buffer_size; a++)					
   				    {  
					ENC_DEBUG ("%2x, ", eth_buffer [a]);
					if (0xf == (a % 16)) 
					   {
						 ENC_DEBUG ("\r\n");
					   }
					}
			
			if ( 's' == eth_buffer[44])
			{
		    	if ( '0' == eth_buffer [UDP_DATA_START])
		       {
			        usart_write("\r\nSize of received UDP packed is %i bytes \r\n",eth_buffer_size);
			        usart_write("Payload_len = %i bytes options =%i ints\r\n", payload_len, options_len);      
			        IRC_options ((unsigned int*)&eth_buffer [UDP_OPTIONS_OFFSET],options_len);
					udp_discovery_packet (myip,mymac,eth_IP_dst,eth_MAC_dst,src_port,dst_port, command_IRC,sizeof (command_IRC));
				}
			
			   if ( '1' == eth_buffer [UDP_DATA_START])
			    {
			           PORTA |= 0b00000001; 
				       udp_discovery_packet (myip,mymac,eth_IP_dst,eth_MAC_dst,src_port,dst_port, command_LED_On,sizeof (command_LED_On));
				}
			    if ( '2' == eth_buffer [UDP_DATA_START])		
				{
			        PORTA &= ~0b00000001;
					udp_discovery_packet (myip,mymac,eth_IP_dst,eth_MAC_dst,src_port,dst_port, command_LED_Off,sizeof (command_LED_Off));
				}
				if ( '3' == eth_buffer [UDP_DATA_START])
			    {
				    IRC_options (IRC_code_array,67);
			        // PORTA |= 0b00000001; 
				    udp_discovery_packet (myip,mymac,eth_IP_dst,eth_MAC_dst,src_port,dst_port, command_IRC,sizeof (command_IRC));
				}
			}	
			 
			else 
			{
			    
			    
			
		  	    if ( '0'== eth_buffer [UDP_DATA_START])
				    {
				       udp_discovery_packet (myip,mymac,eth_IP_dst,eth_MAC_dst,src_port,dst_port,command_list,sizeof (command_list));
					};
					
				if  ( '1'== eth_buffer [UDP_DATA_START])
				    {
                       udp_discovery_packet (myip,mymac,eth_IP_dst,eth_MAC_dst,src_port,dst_port, command_LED_On,sizeof (command_LED_On));
 					};	

                if  ( '2'== eth_buffer [UDP_DATA_START])
				    {
                       udp_discovery_packet (myip,mymac,eth_IP_dst,eth_MAC_dst,src_port,dst_port, command_LED_Off,sizeof (command_LED_Off));
					};						
				if  ( '3'== eth_buffer [UDP_DATA_START])
				    {
					 ADC_data = read_adc(ADC_IN7) - 17;
	                 sprintf((char *) data_string, "ADC=%d", ADC_data);
					 udp_discovery_packet (myip,mymac,eth_IP_dst,eth_MAC_dst,src_port,dst_port,data_string,sizeof (data_string));
					}
			
			
			}
			
			
			}
			eth_buffer_size = 0; // means packet is handled;
			
		}
        //Terminalcommandos auswerten
		if (usart_status.usart_ready){
            usart_write(" Command \r\n");
			if(extract_cmd(&usart_rx_buffer[0]))
			{
				usart_write("Ready\r\n");
			}
			else
			{
				usart_write("ERROR\r\n");
			}
			usart_status.usart_ready =0;
		}
		
        //Wetterdaten empfangen (Testphase)
        #if GET_WEATHER
        http_request ();
        #endif
        
        //Empfang von Zeitinformationen
		#if USE_NTP
		if(!ntp_timer){
			ntp_timer = NTP_REFRESH;
			ntp_request();
		}
		#endif //USE_NTP
		
        //Versand von E-Mails
        #if USE_MAIL
        if (mail_enable == 1)
        {
            mail_enable = 0;
            mail_send();
        }
        #endif //USE_MAIL
        
        //Rechner im Netzwerk aufwecken
        #if USE_WOL
        if (wol_enable == 1)
        {
            wol_enable = 0;
            wol_request();
        }
        #endif //USE_WOL
        
        #if USE_DHCP
        if ( dhcp() != 0) //check for lease timeout
        {
            usart_write("dhcp lease renewal failed\r\n");
			RESET();
        }
        #endif //USE_DHCP
  
		//USART Daten für Telnetanwendung?
		telnetd_send_data();
        
        if(ping.result)
        {
            usart_write("Get PONG: %i.%i.%i.%i\r\n",ping.ip1[0],ping.ip1[1],ping.ip1[2],ping.ip1[3]); 
            ping.result = 0;
        }
    }//while (1)
		
return(0);
}

