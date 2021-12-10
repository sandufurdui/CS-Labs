using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Mail;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace TwoFactorEmail
{

    public partial class MainWindow : Window
    {
        public static string smsBody = "";
        public MainWindow()
        {
            InitializeComponent();
        }

        private void TextBox1_TextChanged(object sender, TextChangedEventArgs e)
        {

        }

        private void TextBox2_TextChanged(object sender, TextChangedEventArgs e)
        {

        }

        private void TextBox3_TextChanged(object sender, TextChangedEventArgs e)
        {

        }
        private void Button_Click(object sender, RoutedEventArgs e)
        {
            Window1 secondWindow = new Window1();
            secondWindow.Show();
            smsBody = GenerateWord(7);
            try
            {
             
                var client = new SmtpClient("smtp.gmail.com", 587)
                {
                    Credentials = new NetworkCredential("test001@gmail.com", "testtest"),
                    EnableSsl = true
                };
               
                try
                {

                    client.Send("test001@gmail.com", emailField.Text, "Verification code", "It is your verification code: " + smsBody);

                }
                catch (SmtpFailedRecipientsException ex)
                {
                    for (int i = 0; i < ex.InnerExceptions.Length; i++)
                    {
                        SmtpStatusCode status = ex.InnerExceptions[i].StatusCode;
                        if (status == SmtpStatusCode.MailboxBusy ||
                            status == SmtpStatusCode.MailboxUnavailable)
                        {
                            Console.WriteLine("Delivery failed - retrying in 5 seconds.");
                            System.Threading.Thread.Sleep(5000);
                            client.Send("test001@gmail.com", emailField.Text, "Verification code", smsBody);
                        }
                        else
                        {
                            Console.WriteLine("Failed to deliver message to {0}",
                                ex.InnerExceptions[i].FailedRecipient);
                        }
                    }
                }
            }
            catch (Exception) { }
            this.Close();
        }
          
        public static string GenerateWord(int length)
        {
            string word = "";
            char[] letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".ToCharArray();

            Random rand = new Random();

            for (int i = 1; i <= 1; i++)
            {
                for (int j = 1; j <= length; j++)
                {
                    int letter_num = rand.Next(0, letters.Length - 1);
                    word += letters[letter_num];
                }
            }
            return word;
        }

    }
}
