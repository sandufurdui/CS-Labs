using System;
using System.Collections.Generic;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;

namespace TwoFactorEmail
{
    public partial class Window1 : Window
    {
        public Boolean isVerified;
        public Window1()
        {
            InitializeComponent();
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            if (isVerified)
            {
                Window2 thirdWindow = new Window2();
                thirdWindow.Show();
                this.Close();
            }
            else 
            {
                objTextBox.BorderBrush = new SolidColorBrush(Colors.Red);
            }
            
          
        }

        private void TextBox_TextChanged(object sender, TextChangedEventArgs e)
        {
            
            string confirmationCode;
            confirmationCode = objTextBox.Text;
            if (confirmationCode != null && MainWindow.smsBody.Trim().Equals(confirmationCode)) {
                isVerified = true;
            }
        }
    }
}
